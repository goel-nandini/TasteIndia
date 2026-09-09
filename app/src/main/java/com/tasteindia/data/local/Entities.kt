package com.tasteindia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A meal as stored on device. Ingredients and tags are already normalised —
 * cleaning happens once, on the way in, not on every render.
 *
 * Only meals from `filter.php?a=Indian` are ever inserted, so this table *is*
 * the Indian boundary.
 */
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val category: String?,
    val area: String?,
    val instructions: String?,
    val ingredients: List<IngredientRecord>,
    val tags: List<String>,
    val sourceUrl: String?,
    val youtubeUrl: String?,
    /** False while we only hold the list-level summary from `filter.php`. */
    val detailsLoaded: Boolean,
)

/**
 * Persisted ingredient row. Deliberately separate from the domain `Ingredient`
 * so a storage-format change cannot ripple into the UI.
 */
@kotlinx.serialization.Serializable
data class IngredientRecord(
    val name: String,
    val measure: String,
)

/**
 * Favourites live in their own table rather than as a column on [MealEntity].
 *
 * That keeps them independent of the recipe cache: clearing or re-syncing meals
 * cannot drop a saved recipe, and the favourite toggle never needs the network
 * or a loaded meal record to work.
 */
@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val mealId: String,
    val savedAt: Long,
)
