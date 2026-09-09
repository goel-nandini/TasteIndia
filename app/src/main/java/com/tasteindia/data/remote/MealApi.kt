package com.tasteindia.data.remote

import com.tasteindia.data.remote.dto.CategoryListResponse
import com.tasteindia.data.remote.dto.MealSummariesResponse
import com.tasteindia.data.remote.dto.MealsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TheMealDB V1, public educational test key `1`.
 *
 * Base URL is `https://www.themealdb.com/api/json/v1/1/`. Query values are
 * passed through Retrofit's `@Query`, which URL-encodes them — ingredient names
 * such as "Garam Masala" contain spaces, so this matters.
 */
interface MealApi {

    /** The authoritative Indian collection. Everything else is filtered from this set. */
    @GET("filter.php")
    suspend fun filterByArea(@Query("a") area: String): MealSummariesResponse

    /** The complete record for one meal, by its stable id. */
    @GET("lookup.php")
    suspend fun lookupMeal(@Query("i") id: String): MealsResponse

    /** Documented category names, used to validate the categories we derive locally. */
    @GET("list.php")
    suspend fun listCategories(@Query("c") list: String = "list"): CategoryListResponse

    companion object {
        const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"
        const val AREA_INDIAN = "Indian"
    }
}
