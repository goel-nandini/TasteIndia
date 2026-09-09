package com.tasteindia.data.repository

/**
 * Failure kinds the UI actually treats differently. Deliberately small — a
 * reader gets one of four messages, not a stack trace, and each one implies a
 * different user action.
 */
sealed interface AppError {
    /** No usable connection. Retry will not help until that changes. */
    data object Offline : AppError

    /** Reached the network but the request did not complete in time. */
    data object Timeout : AppError

    /** TheMealDB answered with a non-2xx status. */
    data class Server(val code: Int) : AppError

    /** Anything else — malformed payload, unexpected exception. */
    data class Unexpected(val message: String?) : AppError
}

/** State of the collection-level refresh, distinct from per-meal detail loading. */
sealed interface SyncState {
    data object Idle : SyncState
    data object Loading : SyncState
    data object Success : SyncState
    data class Failed(val error: AppError) : SyncState
}

/**
 * Progress of the detail-enrichment pass.
 *
 * Exposed separately from [SyncState] so the list can be scrollable and usable
 * while details are still arriving — the brief forbids an indefinite spinner,
 * and "23 of 60" is more honest than a bar that cannot finish.
 */
data class EnrichmentProgress(
    val completed: Int = 0,
    val total: Int = 0,
) {
    val isRunning: Boolean get() = total > 0 && completed < total
}
