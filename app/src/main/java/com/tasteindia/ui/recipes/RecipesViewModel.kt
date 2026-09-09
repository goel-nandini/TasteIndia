package com.tasteindia.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteindia.data.repository.AppError
import com.tasteindia.data.repository.EnrichmentProgress
import com.tasteindia.data.repository.MealRepository
import com.tasteindia.data.repository.SyncState
import com.tasteindia.domain.MealFilters
import com.tasteindia.domain.model.FilterState
import com.tasteindia.domain.model.Meal
import com.tasteindia.domain.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Everything the Recipes screen renders, as one immutable value.
 *
 * Exposing a single state object rather than several flows means the screen can
 * never show a half-updated combination — the result count always matches the
 * list it describes.
 */
data class RecipesUiState(
    val meals: List<Meal> = emptyList(),
    val totalCount: Int = 0,
    val filter: FilterState = FilterState(),
    val categoryOptions: List<String> = emptyList(),
    val ingredientOptions: List<String> = emptyList(),
    val enrichment: EnrichmentProgress = EnrichmentProgress(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
) {
    /** True only when there is genuinely nothing stored yet and we are fetching. */
    val showFullScreenLoading: Boolean get() = isLoading && totalCount == 0

    /** A failed refresh with nothing cached is a dead end; with cache it is a banner. */
    val showFullScreenError: Boolean get() = error != null && totalCount == 0
    val showOfflineBanner: Boolean get() = error != null && totalCount > 0

    val showEmptyFilterResult: Boolean get() = meals.isEmpty() && totalCount > 0 && !isLoading
}

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val repository: MealRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * The source of truth for search, filters and sort order.
     *
     * Seeded from [SavedStateHandle] and written back on every change, so the
     * screen returns from details — or from process death — with exactly the
     * filters the user left applied.
     */
    private val filterState = MutableStateFlow(savedStateHandle.readFilterState())

    val uiState: StateFlow<RecipesUiState> = combine(
        repository.observeMeals(),
        filterState,
        repository.syncState,
        repository.enrichment,
        repository.documentedCategories,
    ) { meals, filter, sync, enrichment, documentedCategories ->
        RecipesUiState(
            // One pure function decides what is visible. Because it runs over an
            // already-loaded list there is no request to cancel and no response
            // that can arrive out of order — a stale result is not possible.
            meals = MealFilters.apply(meals, filter),
            totalCount = meals.size,
            filter = filter,
            categoryOptions = MealFilters.categoryOptions(meals, documentedCategories),
            ingredientOptions = MealFilters.ingredientOptions(meals),
            enrichment = enrichment,
            isLoading = sync is SyncState.Loading,
            error = (sync as? SyncState.Failed)?.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecipesUiState(filter = filterState.value, isLoading = true),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun onQueryChange(query: String) = updateFilter { it.copy(query = query) }

    fun onCategoryChange(category: String?) = updateFilter { it.copy(category = category) }

    fun onIngredientChange(ingredient: String?) = updateFilter { it.copy(ingredient = ingredient) }

    fun onFavouritesOnlyChange(enabled: Boolean) = updateFilter { it.copy(favouritesOnly = enabled) }

    fun onSortOrderChange(order: SortOrder) = updateFilter { it.copy(sortOrder = order) }

    fun onClearAllFilters() = updateFilter { it.cleared() }

    fun onToggleFavourite(mealId: String) {
        viewModelScope.launch { repository.toggleFavourite(mealId) }
    }

    private fun updateFilter(transform: (FilterState) -> FilterState) {
        filterState.update { current ->
            transform(current).also { savedStateHandle.writeFilterState(it) }
        }
    }
}

// --- SavedStateHandle persistence -------------------------------------------
//
// Stored as primitives rather than as a serialized object: SavedStateHandle is
// backed by a Bundle, and primitives survive process death without needing the
// model class to stay Parcelable-compatible across versions.

private const val KEY_QUERY = "filter_query"
private const val KEY_CATEGORY = "filter_category"
private const val KEY_INGREDIENT = "filter_ingredient"
private const val KEY_FAVOURITES_ONLY = "filter_favourites_only"
private const val KEY_SORT = "filter_sort"

private fun SavedStateHandle.readFilterState(): FilterState = FilterState(
    query = get<String>(KEY_QUERY).orEmpty(),
    category = get<String>(KEY_CATEGORY),
    ingredient = get<String>(KEY_INGREDIENT),
    favouritesOnly = get<Boolean>(KEY_FAVOURITES_ONLY) ?: false,
    sortOrder = get<String>(KEY_SORT)
        ?.let { name -> runCatching { SortOrder.valueOf(name) }.getOrNull() }
        ?: SortOrder.NAME_ASC,
)

private fun SavedStateHandle.writeFilterState(state: FilterState) {
    set(KEY_QUERY, state.query)
    set(KEY_CATEGORY, state.category)
    set(KEY_INGREDIENT, state.ingredient)
    set(KEY_FAVOURITES_ONLY, state.favouritesOnly)
    set(KEY_SORT, state.sortOrder.name)
}
