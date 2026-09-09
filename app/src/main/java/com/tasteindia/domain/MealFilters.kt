package com.tasteindia.domain

import com.tasteindia.domain.model.FilterState
import com.tasteindia.domain.model.Meal
import com.tasteindia.domain.model.SortOrder

/**
 * The whole of the list's filtering and sorting behaviour, as one pure function
 * over an in-memory list.
 *
 * This is deliberately not a database query and not a network call. Because the
 * repository stores the complete Indian collection locally, combining category,
 * ingredient, favourites and search needs no request at all — which is what
 * makes the "Indian + category + ingredient" intersection that TheMealDB cannot
 * express in one endpoint trivially correct here, and exhaustively testable.
 *
 * The Indian boundary is not checked in this file on purpose: a non-Indian meal
 * can never be in [meals], because only `filter.php?a=Indian` results are ever
 * inserted into the database.
 */
object MealFilters {

    fun apply(meals: List<Meal>, state: FilterState): List<Meal> {
        val query = state.query.trim()

        val filtered = meals.filter { meal ->
            matchesQuery(meal, query) &&
                matchesCategory(meal, state.category) &&
                matchesIngredient(meal, state.ingredient) &&
                matchesFavourites(meal, state.favouritesOnly)
        }

        return sort(filtered, state.sortOrder)
    }

    private fun matchesQuery(meal: Meal, query: String): Boolean =
        query.isEmpty() || meal.name.contains(query, ignoreCase = true)

    private fun matchesCategory(meal: Meal, category: String?): Boolean =
        category == null || meal.category.equals(category, ignoreCase = true)

    private fun matchesIngredient(meal: Meal, ingredient: String?): Boolean =
        ingredient == null || meal.ingredients.any { it.name.equals(ingredient, ignoreCase = true) }

    private fun matchesFavourites(meal: Meal, favouritesOnly: Boolean): Boolean =
        !favouritesOnly || meal.isFavourite

    private fun sort(meals: List<Meal>, order: SortOrder): List<Meal> =
        when (order) {
            SortOrder.NAME_ASC -> meals.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortOrder.NAME_DESC -> meals.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
        }

    /**
     * Category choices offered to the user, derived from meals we have actually
     * loaded details for.
     *
     * Deriving them from the data rather than from `list.php?c=list` guarantees
     * every option returns at least one result. [documented] is the set returned
     * by TheMealDB's own category list; when it is available we intersect with
     * it so we never offer a value the provider does not recognise. When it is
     * not available (offline, or that call failed) we fall back to the derived
     * set rather than showing nothing.
     */
    fun categoryOptions(meals: List<Meal>, documented: Set<String>): List<String> {
        val derived = meals.mapNotNull { it.category }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        val validated = if (documented.isEmpty()) {
            derived
        } else {
            val lowered = documented.map { it.lowercase() }.toSet()
            derived.filter { it.lowercase() in lowered }
        }

        return validated.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    /** Main-ingredient choices, derived from the loaded collection for the same reason. */
    fun ingredientOptions(meals: List<Meal>): List<String> =
        meals.flatMap { meal -> meal.ingredients.map { it.name } }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
}
