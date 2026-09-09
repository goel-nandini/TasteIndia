package com.tasteindia.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Ingredients and tags are stored as JSON in a single column.
 *
 * A child table would be more normalised, but nothing in this app ever queries
 * ingredients in SQL — filtering happens in memory over the whole ~60-meal
 * collection — so a join table would add schema and migration surface for no
 * behaviour. Documented as a deliberate tradeoff in the README.
 */
class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun ingredientsToJson(value: List<IngredientRecord>): String =
        json.encodeToString(ListSerializer(IngredientRecord.serializer()), value)

    @TypeConverter
    fun jsonToIngredients(value: String): List<IngredientRecord> =
        runCatching {
            json.decodeFromString(ListSerializer(IngredientRecord.serializer()), value)
        }.getOrDefault(emptyList())

    @TypeConverter
    fun tagsToJson(value: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun jsonToTags(value: String): List<String> =
        runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), value)
        }.getOrDefault(emptyList())
}

@Database(
    entities = [MealEntity::class, FavouriteEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun favouriteDao(): FavouriteDao

    companion object {
        const val NAME = "tasteindia.db"
    }
}
