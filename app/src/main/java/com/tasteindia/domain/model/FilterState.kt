package com.tasteindia.domain.model

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
}

/**
 * Everything the user can change about how the Recipes list is presented.
 *
 * This is the single source of truth for search, filters and sort order. It is
 * owned by the ViewModel and mirrored into `SavedStateHandle`, so it survives
 * navigation, configuration change and process death.
 *
 * Note there is no `area` filter: the Indian area is a hard boundary enforced by
 * the repository (only Indian meals are ever stored), not a user-adjustable
 * value. See the README for that decision.
 */
data class FilterState(
    val query: String = "",
    val category: String? = null,
    val ingredient: String? = null,
    val favouritesOnly: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
) {
    /** Sort order is always set, so it never counts as an "active filter". */
    val activeFilterCount: Int
        get() = listOf(
            query.isNotBlank(),
            category != null,
            ingredient != null,
            favouritesOnly,
        ).count { it }

    val hasActiveFilters: Boolean get() = activeFilterCount > 0

    /** Clears filters but deliberately keeps the chosen sort order. */
    fun cleared(): FilterState = FilterState(sortOrder = sortOrder)
}
