package com.tasteindia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Query("SELECT * FROM meals")
    fun observeAll(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<MealEntity?>

    @Query("SELECT * FROM meals WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): MealEntity?

    @Query("SELECT id FROM meals WHERE detailsLoaded = 0")
    suspend fun idsMissingDetails(): List<String>

    @Query("SELECT COUNT(*) FROM meals")
    suspend fun count(): Int

    /**
     * Summaries from `filter.php` must never overwrite a record we have already
     * enriched, so this ignores conflicts. Full records use [upsert] instead.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSummaries(meals: List<MealEntity>)

    @Upsert
    suspend fun upsert(meal: MealEntity)
}

@Dao
interface FavouriteDao {

    @Query("SELECT mealId FROM favourites")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE mealId = :id)")
    suspend fun isFavourite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favourite: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE mealId = :id")
    suspend fun remove(id: String)
}
