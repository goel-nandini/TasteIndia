package com.tasteindia.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasteindia.R
import com.tasteindia.data.repository.AppError
import com.tasteindia.domain.model.FilterState
import com.tasteindia.domain.model.Meal
import com.tasteindia.ui.common.EmptyState
import com.tasteindia.ui.common.EnrichmentBar
import com.tasteindia.ui.common.ErrorState
import com.tasteindia.ui.common.LoadingState
import com.tasteindia.ui.common.MealRow
import com.tasteindia.ui.common.OfflineBanner
import com.tasteindia.ui.common.toMessage
import com.tasteindia.ui.theme.TasteIndiaTheme

@Composable
fun RecipesRoute(
    onMealClick: (String) -> Unit,
    viewModel: RecipesViewModel = hiltViewModel(),
) {
    // Lifecycle-aware collection: the flow stops being observed when the screen
    // is not visible, and resumes without rebuilding the ViewModel.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RecipesScreen(
        state = uiState,
        onMealClick = onMealClick,
        onToggleFavourite = viewModel::onToggleFavourite,
        onQueryChange = viewModel::onQueryChange,
        onCategoryChange = viewModel::onCategoryChange,
        onIngredientChange = viewModel::onIngredientChange,
        onFavouritesOnlyChange = viewModel::onFavouritesOnlyChange,
        onSortOrderChange = viewModel::onSortOrderChange,
        onClearAllFilters = viewModel::onClearAllFilters,
        onRetry = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    state: RecipesUiState,
    onMealClick: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onIngredientChange: (String?) -> Unit,
    onFavouritesOnlyChange: (Boolean) -> Unit,
    onSortOrderChange: (com.tasteindia.domain.model.SortOrder) -> Unit,
    onClearAllFilters: () -> Unit,
    onRetry: () -> Unit,
) {
    // Survives configuration change and process death, so returning to the list
    // restores the exact scroll offset rather than jumping to the top.
    val listState = rememberLazyListState()
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.destination_recipes)) },
                actions = {
                    val filterLabel = stringResource(R.string.filters_open)
                    BadgedBox(
                        badge = {
                            if (state.filter.activeFilterCount > 0) {
                                Badge { Text(state.filter.activeFilterCount.toString()) }
                            }
                        },
                    ) {
                        IconButton(onClick = { showFilters = true }) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = filterLabel,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            SearchField(
                query = state.filter.query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            ActiveFilterChips(
                filter = state.filter,
                onCategoryChange = onCategoryChange,
                onIngredientChange = onIngredientChange,
                onFavouritesOnlyChange = onFavouritesOnlyChange,
                onQueryChange = onQueryChange,
                onClearAll = onClearAllFilters,
            )

            if (state.showOfflineBanner) {
                OfflineBanner(message = stringResource(R.string.state_offline_banner))
            }

            if (state.enrichment.isRunning) {
                EnrichmentBar(
                    completed = state.enrichment.completed,
                    total = state.enrichment.total,
                )
            }

            when {
                state.showFullScreenLoading -> LoadingState()

                state.showFullScreenError -> ErrorState(
                    message = state.error!!.toMessage(),
                    onRetry = onRetry,
                )

                state.showEmptyFilterResult -> EmptyState(
                    title = stringResource(R.string.state_empty_filters_title),
                    body = stringResource(R.string.state_empty_filters_body),
                    action = {
                        TextButton(onClick = onClearAllFilters) {
                            Text(stringResource(R.string.filters_clear_all))
                        }
                    },
                )

                else -> {
                    ResultCount(shown = state.meals.size, total = state.totalCount)
                    HorizontalDivider()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        // Keyed on the provider's stable meal id — never on list
                        // position, so favouriting or re-sorting cannot rebind a
                        // row to the wrong recipe.
                        items(items = state.meals, key = { it.id }) { meal ->
                            MealRow(
                                meal = meal,
                                onClick = { onMealClick(meal.id) },
                                onToggleFavourite = { onToggleFavourite(meal.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            state = state.filter,
            categoryOptions = state.categoryOptions,
            ingredientOptions = state.ingredientOptions,
            onCategoryChange = onCategoryChange,
            onIngredientChange = onIngredientChange,
            onFavouritesOnlyChange = onFavouritesOnlyChange,
            onSortOrderChange = onSortOrderChange,
            onClearAll = onClearAllFilters,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_clear),
                    )
                }
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
    )
}

/**
 * The applied filters, always visible above the list so the user can see why a
 * result set looks the way it does — and remove any one of them in a single tap.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ActiveFilterChips(
    filter: FilterState,
    onCategoryChange: (String?) -> Unit,
    onIngredientChange: (String?) -> Unit,
    onFavouritesOnlyChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    if (!filter.hasActiveFilters) return

    val label = stringResource(R.string.filters_active_label)

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (filter.query.isNotBlank()) {
            RemovableFilterChip(
                text = stringResource(R.string.filter_chip_search, filter.query),
                onRemove = { onQueryChange("") },
            )
        }
        filter.category?.let { category ->
            RemovableFilterChip(
                text = stringResource(R.string.filter_chip_category, category),
                onRemove = { onCategoryChange(null) },
            )
        }
        filter.ingredient?.let { ingredient ->
            RemovableFilterChip(
                text = stringResource(R.string.filter_chip_ingredient, ingredient),
                onRemove = { onIngredientChange(null) },
            )
        }
        if (filter.favouritesOnly) {
            RemovableFilterChip(
                text = stringResource(R.string.filter_chip_favourites),
                onRemove = { onFavouritesOnlyChange(false) },
            )
        }
        TextButton(onClick = onClearAll) {
            Text(stringResource(R.string.filters_clear_all))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemovableFilterChip(text: String, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(text) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.filter_chip_remove, text),
            )
        },
    )
}

@Composable
private fun ResultCount(shown: Int, total: Int) {
    val shownText = pluralStringResource(R.plurals.result_count, shown, shown)
    val text = if (shown == total) shownText else stringResource(R.string.result_count_of_total, shownText, total)

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

// --- Previews ---------------------------------------------------------------

private fun previewMeal(id: String, name: String, category: String, favourite: Boolean = false) = Meal(
    id = id,
    name = name,
    thumbnailUrl = null,
    category = category,
    area = "Indian",
    instructions = null,
    ingredients = emptyList(),
    tags = emptyList(),
    sourceUrl = null,
    youtubeUrl = null,
    detailsLoaded = true,
    isFavourite = favourite,
)

private val previewMeals = listOf(
    previewMeal("52795", "Chicken Handi", "Chicken"),
    previewMeal("52804", "Rogan Josh", "Lamb", favourite = true),
    previewMeal("52785", "Dal Fry", "Vegetarian"),
)

@Preview(name = "Loaded", showBackground = true)
@Composable
private fun RecipesLoadedPreview() {
    TasteIndiaTheme {
        RecipesScreen(
            state = RecipesUiState(meals = previewMeals, totalCount = previewMeals.size),
            onMealClick = {}, onToggleFavourite = {}, onQueryChange = {},
            onCategoryChange = {}, onIngredientChange = {}, onFavouritesOnlyChange = {},
            onSortOrderChange = {}, onClearAllFilters = {}, onRetry = {},
        )
    }
}

@Preview(name = "Filtered to nothing", showBackground = true)
@Composable
private fun RecipesEmptyFilterPreview() {
    TasteIndiaTheme {
        RecipesScreen(
            state = RecipesUiState(
                meals = emptyList(),
                totalCount = 60,
                filter = FilterState(query = "pizza", category = "Dessert"),
            ),
            onMealClick = {}, onToggleFavourite = {}, onQueryChange = {},
            onCategoryChange = {}, onIngredientChange = {}, onFavouritesOnlyChange = {},
            onSortOrderChange = {}, onClearAllFilters = {}, onRetry = {},
        )
    }
}

@Preview(name = "Offline with cache", showBackground = true)
@Composable
private fun RecipesOfflinePreview() {
    TasteIndiaTheme {
        RecipesScreen(
            state = RecipesUiState(
                meals = previewMeals,
                totalCount = previewMeals.size,
                error = AppError.Offline,
            ),
            onMealClick = {}, onToggleFavourite = {}, onQueryChange = {},
            onCategoryChange = {}, onIngredientChange = {}, onFavouritesOnlyChange = {},
            onSortOrderChange = {}, onClearAllFilters = {}, onRetry = {},
        )
    }
}

@Preview(name = "Hard failure, nothing cached", showBackground = true)
@Composable
private fun RecipesErrorPreview() {
    TasteIndiaTheme {
        RecipesScreen(
            state = RecipesUiState(error = AppError.Timeout),
            onMealClick = {}, onToggleFavourite = {}, onQueryChange = {},
            onCategoryChange = {}, onIngredientChange = {}, onFavouritesOnlyChange = {},
            onSortOrderChange = {}, onClearAllFilters = {}, onRetry = {},
        )
    }
}
