package com.tasteindia.data.repository

import com.tasteindia.data.local.FavouriteDao
import com.tasteindia.data.local.FavouriteEntity
import com.tasteindia.data.local.MealDao
import com.tasteindia.data.mapper.toDomain
import com.tasteindia.data.mapper.toEntity
import com.tasteindia.data.remote.MealApi
import com.tasteindia.di.ApplicationScope
import com.tasteindia.di.IoDispatcher
import com.tasteindia.domain.model.Meal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMealRepository @Inject constructor(
    private val api: MealApi,
    private val mealDao: MealDao,
    private val favouriteDao: FavouriteDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val externalScope: CoroutineScope,
) : MealRepository {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _enrichment = MutableStateFlow(EnrichmentProgress())
    override val enrichment: StateFlow<EnrichmentProgress> = _enrichment.asStateFlow()

    private val _documentedCategories = MutableStateFlow<Set<String>>(emptySet())
    override val documentedCategories: StateFlow<Set<String>> = _documentedCategories.asStateFlow()

    /**
     * In-flight detail requests, keyed by meal id.
     *
     * This is the deduplication guarantee: a second caller asking for a meal
     * already being fetched awaits the existing job instead of issuing another
     * request. The list screen, the details screen and the enrichment pass can
     * therefore all ask for the same meal at once and produce exactly one GET.
     */
    private val inFlightDetails = mutableMapOf<String, Deferred<Unit>>()
    private val inFlightLock = Mutex()

    /** Serialises whole-collection refreshes so two callers cannot both sync. */
    private val refreshLock = Mutex()

    override fun observeMeals(): Flow<List<Meal>> =
        combine(
            mealDao.observeAll(),
            favouriteDao.observeIds(),
        ) { meals, favouriteIds ->
            val favourites = favouriteIds.toHashSet()
            meals.map { it.toDomain(isFavourite = favourites.contains(it.id)) }
        }.distinctUntilChanged()

    override fun observeMeal(id: String): Flow<Meal?> =
        combine(
            mealDao.observeById(id),
            favouriteDao.observeIds(),
        ) { meal, favouriteIds ->
            meal?.toDomain(isFavourite = favouriteIds.contains(meal.id))
        }.distinctUntilChanged()

    override suspend fun refresh() {
        // A refresh already running is the same work; don't start a second one.
        if (refreshLock.isLocked) return

        refreshLock.withLock {
            _syncState.value = SyncState.Loading
            try {
                withContext(ioDispatcher) {
                    val summaries = api.filterByArea(MealApi.AREA_INDIAN).meals.orEmpty()
                    if (summaries.isNotEmpty()) {
                        mealDao.insertSummaries(summaries.map { it.toEntity() })
                    }
                    loadDocumentedCategories()
                    enrichMissingDetails()
                }
                _syncState.value = SyncState.Success
            } catch (error: Throwable) {
                _syncState.value = SyncState.Failed(error.toAppError())
            }
        }
    }

    /**
     * Non-fatal: the category list is a nicety used to validate locally derived
     * options. If it fails we keep the derived options rather than failing the
     * whole refresh over it.
     */
    private suspend fun loadDocumentedCategories() {
        runCatching {
            api.listCategories().meals.orEmpty()
                .mapNotNull { it.strCategory?.trim()?.takeIf(String::isNotEmpty) }
                .toSet()
        }.onSuccess { categories ->
            if (categories.isNotEmpty()) _documentedCategories.value = categories
        }
    }

    /**
     * Loads the full record for every meal we only hold a summary for.
     *
     * Bounded by [MAX_CONCURRENT_DETAIL_REQUESTS]: ~60 lookups are issued a few
     * at a time rather than all at once, which is the difference between a
     * controlled enrichment pass and the request storm the brief warns about.
     */
    private suspend fun enrichMissingDetails() {
        val missing = mealDao.idsMissingDetails()
        if (missing.isEmpty()) {
            _enrichment.value = EnrichmentProgress()
            return
        }

        _enrichment.value = EnrichmentProgress(completed = 0, total = missing.size)
        val gate = Semaphore(MAX_CONCURRENT_DETAIL_REQUESTS)

        try {
            coroutineScope {
                missing.map { id ->
                    async {
                        gate.withPermit { loadDetail(id) }
                        _enrichment.update { it.copy(completed = it.completed + 1) }
                    }
                }.forEach { it.await() }
            }
        } finally {
            // Whether it finished or was cancelled, stop reporting progress.
            _enrichment.value = EnrichmentProgress()
        }
    }

    override suspend fun ensureDetails(id: String) {
        val existing = withContext(ioDispatcher) { mealDao.findById(id) }
        if (existing?.detailsLoaded == true) return
        withContext(ioDispatcher) { loadDetail(id) }
    }

    /**
     * Fetches and stores one meal's full record, at most once at a time per id.
     *
     * The job is started on [externalScope] rather than the caller's scope so
     * that a caller going away (a screen closing) does not cancel work another
     * caller is still awaiting.
     */
    private suspend fun loadDetail(id: String) {
        val job = inFlightLock.withLock {
            inFlightDetails[id] ?: externalScope.async(ioDispatcher) {
                try {
                    val dto = api.lookupMeal(id).meals?.firstOrNull()
                    if (dto != null) mealDao.upsert(dto.toEntity())
                } finally {
                    inFlightLock.withLock { inFlightDetails.remove(id) }
                }
            }.also { inFlightDetails[id] = it }
        }
        job.await()
    }

    override suspend fun toggleFavourite(id: String) {
        withContext(ioDispatcher) {
            if (favouriteDao.isFavourite(id)) {
                favouriteDao.remove(id)
            } else {
                favouriteDao.add(FavouriteEntity(mealId = id, savedAt = System.currentTimeMillis()))
            }
        }
    }

    companion object {
        /**
         * Chosen, not arbitrary: OkHttp's default dispatcher allows 5 concurrent
         * requests per host, so a ceiling of 6 keeps the connection pool busy
         * without queueing dozens of calls behind it.
         */
        const val MAX_CONCURRENT_DETAIL_REQUESTS = 6
    }
}

/** Small local helper so progress updates stay atomic under concurrency. */
private inline fun MutableStateFlow<EnrichmentProgress>.update(
    transform: (EnrichmentProgress) -> EnrichmentProgress,
) {
    while (true) {
        val current = value
        if (compareAndSet(current, transform(current))) return
    }
}

internal fun Throwable.toAppError(): AppError = when (this) {
    is UnknownHostException -> AppError.Offline
    is SocketTimeoutException -> AppError.Timeout
    is HttpException -> AppError.Server(code())
    is IOException -> AppError.Offline
    else -> AppError.Unexpected(message)
}
