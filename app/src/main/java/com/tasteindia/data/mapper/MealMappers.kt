package com.tasteindia.data.mapper

import com.tasteindia.data.local.IngredientRecord
import com.tasteindia.data.local.MealEntity
import com.tasteindia.data.remote.dto.MealDto
import com.tasteindia.data.remote.dto.MealSummaryDto
import com.tasteindia.domain.model.Ingredient
import com.tasteindia.domain.model.Meal

/**
 * Trims a provider string and collapses "absent" into null.
 *
 * TheMealDB returns three different things for an empty field — `null`, `""`,
 * and `" "` — and real responses contain all three. Treating them identically
 * here is what stops blank rows and empty labels reaching the UI.
 */
internal fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Splits `strTags` ("Vegetarian,Curry") into a list, dropping empties and
 * duplicates. Returns an empty list for null or blank input.
 */
internal fun String?.toTags(): List<String> =
    this.cleaned()
        ?.split(',')
        ?.mapNotNull { it.cleaned() }
        ?.distinctBy { it.lowercase() }
        .orEmpty()

/**
 * Accepts a value only if it is a syntactically valid http(s) URL.
 *
 * The brief requires links be opened "only if valid"; TheMealDB has records with
 * blank sources and malformed video fields, and handing one of those to an
 * Intent is how an app crashes on a details screen.
 */
internal fun String?.asValidHttpUrl(): String? {
    val candidate = this.cleaned() ?: return null
    val lower = candidate.lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return null
    // Must have something after the scheme separator to be a usable link.
    val afterScheme = candidate.substringAfter("://")
    if (afterScheme.isBlank() || !afterScheme.contains('.')) return null
    return candidate
}

/**
 * Normalises TheMealDB's twenty `strIngredient`/`strMeasure` slots into ordered
 * rows.
 *
 * Rules, each of which corresponds to a real shape in the live data:
 *  - a slot with a blank or whitespace-only ingredient name is dropped entirely,
 *    even when it carries a measure (an orphan measure has nothing to label);
 *  - a slot with a name but no measure is kept, with an empty measure — some
 *    meals list "Coriander" with no quantity, and dropping it would lose a real
 *    ingredient;
 *  - gaps in the middle are removed while the surrounding order is preserved,
 *    because the provider does not always fill slots contiguously;
 *  - all values are trimmed.
 */
fun MealDto.toIngredients(): List<Ingredient> =
    ingredientPairs.mapNotNull { (name, measure) ->
        val cleanName = name.cleaned() ?: return@mapNotNull null
        Ingredient(name = cleanName, measure = measure.cleaned().orEmpty())
    }

fun MealDto.toEntity(): MealEntity = MealEntity(
    id = idMeal,
    // A meal with no name is unusable in a list; fall back to the id rather than
    // rendering an empty row, and let it stay visible so the gap is diagnosable.
    name = strMeal.cleaned() ?: idMeal,
    thumbnailUrl = strMealThumb.asValidHttpUrl(),
    category = strCategory.cleaned(),
    area = strArea.cleaned(),
    instructions = strInstructions.cleaned(),
    ingredients = toIngredients().map { IngredientRecord(it.name, it.measure) },
    tags = strTags.toTags(),
    sourceUrl = strSource.asValidHttpUrl(),
    youtubeUrl = strYoutube.asValidHttpUrl(),
    detailsLoaded = true,
)

/**
 * The list-level record. [MealEntity.detailsLoaded] is false, which is what the
 * repository's enrichment pass looks for.
 */
fun MealSummaryDto.toEntity(): MealEntity = MealEntity(
    id = idMeal,
    name = strMeal.cleaned() ?: idMeal,
    thumbnailUrl = strMealThumb.asValidHttpUrl(),
    category = null,
    area = null,
    instructions = null,
    ingredients = emptyList(),
    tags = emptyList(),
    sourceUrl = null,
    youtubeUrl = null,
    detailsLoaded = false,
)

fun MealEntity.toDomain(isFavourite: Boolean): Meal = Meal(
    id = id,
    name = name,
    thumbnailUrl = thumbnailUrl,
    category = category,
    area = area,
    instructions = instructions,
    ingredients = ingredients.map { Ingredient(it.name, it.measure) },
    tags = tags,
    sourceUrl = sourceUrl,
    youtubeUrl = youtubeUrl,
    detailsLoaded = detailsLoaded,
    isFavourite = isFavourite,
)
