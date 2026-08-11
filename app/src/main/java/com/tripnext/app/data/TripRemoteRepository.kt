package com.tripnext.app.data

import com.tripnext.app.data.local.PendingOperationEntity

/** Boundary for the phase-2 API. Implementations must be idempotent by deduplicationKey. */
interface TripRemoteRepository {
    suspend fun push(operation: PendingOperationEntity): Result<Unit>
}

class OfflineTripRemoteRepository : TripRemoteRepository {
    override suspend fun push(operation: PendingOperationEntity) = Result.failure<Unit>(IllegalStateException("Backend not configured"))
}
