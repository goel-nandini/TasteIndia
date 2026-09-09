package com.tasteindia.data.repository

import com.tasteindia.domain.model.Meal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The single source of truth for meals and favourites.
 *
 * Reads are cold [Flow]s backed by the database, never by the network: the app
 * renders whatever is stored, and refreshing only updates that store. This is
 * what makes a cold launch with no connection work.
 */
interface MealRepository {

    /** Every stored Indian meal, joined with its favourite status. */
    fun observeMeals(): Flow<List<Meal>>

    /** One meal by stable id; emits null when it is not in the store. */
    fun observeMeal(id: String): Flow<Meal?>

    val syncState: StateFlow<SyncState>

    val enrichment: StateFlow<EnrichmentProgress>

    /** Category names as documented by TheMealDB; empty when that call has not succeeded. */
    val documentedCategories: StateFlow<Set<String>>

    /**
     * Refreshes the Indian collection and enriches any meal missing details.
     * Safe to call repeatedly; concurrent calls collapse into one.
     */
    suspend fun refresh()

    /** Ensures one meal's full record is loaded, if it is not already. */
    suspend fun ensureDetails(id: String)

    /** Adds or removes a favourite. Never touches the network. */
    suspend fun toggleFavourite(id: String)
}
