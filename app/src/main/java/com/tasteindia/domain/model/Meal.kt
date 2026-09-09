package com.tasteindia.domain.model

/**
 * A single ingredient row, already normalised out of TheMealDB's
 * `strIngredient1..20` / `strMeasure1..20` field pairs.
 *
 * A row only exists if it has a non-blank [name]; [measure] may legitimately be
 * empty (some meals list an ingredient with no quantity), and the UI renders
 * that as an empty cell rather than a blank label.
 */
data class Ingredient(
    val name: String,
    val measure: String,
)

/**
 * The shape the UI consumes. Every field here is either non-null or explicitly
 * optional — by the time a [Meal] exists, blank strings, whitespace-only values
 * and malformed URLs have already been removed by the mappers.
 *
 * @param detailsLoaded false when we only have the list-level summary
 *   (id, name, thumbnail) from `filter.php` and the full `lookup.php` record has
 *   not arrived yet.
 */
data class Meal(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val category: String?,
    val area: String?,
    val instructions: String?,
    val ingredients: List<Ingredient>,
    val tags: List<String>,
    val sourceUrl: String?,
    val youtubeUrl: String?,
    val detailsLoaded: Boolean,
    val isFavourite: Boolean,
)
