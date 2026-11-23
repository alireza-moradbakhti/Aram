package com.aram.core.network

import com.aram.core.model.AppError
import com.aram.core.network.api.ApiError
import com.aram.core.network.api.toAppError
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorMappingTest {

    @Test
    fun `every transport error maps to its AppError kind`() {
        assertEquals(AppError.Offline, ApiError.Offline.toAppError())
        assertEquals(AppError.Timeout, ApiError.Timeout.toAppError())
        assertEquals(AppError.Unauthorized, ApiError.Unauthorized.toAppError())
        assertEquals(AppError.Forbidden, ApiError.Forbidden.toAppError())
        assertEquals(AppError.NotFound, ApiError.NotFound.toAppError())
        assertEquals(AppError.Conflict("stale"), ApiError.Conflict("stale").toAppError())
        assertEquals(AppError.RateLimited(9), ApiError.RateLimited(9).toAppError())
        assertEquals(AppError.Server("req-1"), ApiError.Server("req-1").toAppError())
        assertEquals(
            AppError.Validation(mapOf("email" to "invalid")),
            ApiError.Validation(mapOf("email" to "invalid")).toAppError(),
        )
        assertEquals(AppError.Unknown(), ApiError.Unknown().toAppError())
    }

    @Test
    fun `conflict and server keep their message`() {
        assertEquals("stale", (ApiError.Conflict("stale").toAppError() as AppError.Conflict).message)
        assertEquals(null, (ApiError.Conflict().toAppError() as AppError.Conflict).message)
    }
}
