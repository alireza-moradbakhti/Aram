package com.aram.core.common.error

import com.aram.core.logging.LogConfig
import com.aram.core.logging.LogEvent
import com.aram.core.logging.LogLevel
import com.aram.core.logging.LogSink
import com.aram.core.logging.Logger
import com.aram.core.model.AppError
import com.aram.core.model.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMappingTest {

    private val sink = RecordingSink()
    private val logger = Logger(
        config = LogConfig(minLevel = LogLevel.VERBOSE),
        sinks = listOf(sink),
    )

    @Test
    fun `illegal argument maps to validation`() {
        assertEquals(AppError.Validation(), IllegalArgumentException().toAppError())
    }

    @Test
    fun `security exception maps to permission`() {
        assertEquals(AppError.Permission, SecurityException().toAppError())
    }

    @Test
    fun `other throwables map to unknown with the original cause`() {
        val cause = IllegalStateException("boom")

        val error = cause.toAppError()

        assertTrue(error is AppError.Unknown)
        assertSame(cause, (error as AppError.Unknown).cause)
    }

    @Test
    fun `safeOutcome returns success without logging`() = runBlocking {
        val result = safeOutcome("test", "op", logger) { 42 }

        assertEquals(Outcome.Success(42), result)
        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun `safeOutcome logs once and returns the mapped failure`() = runBlocking {
        val cause = IllegalStateException("boom")

        val result = safeOutcome("test", "op", logger) { throw cause }

        assertTrue(result is Outcome.Failure)
        assertTrue((result as Outcome.Failure).error is AppError.Unknown)
        assertEquals(1, sink.events.size)
        assertEquals(LogLevel.ERROR, sink.events.single().level)
        assertSame(cause, sink.events.single().throwable)
    }

    @Test
    fun `safeOutcome honors a custom mapper`() = runBlocking {
        val result = safeOutcome(
            component = "test",
            operation = "op",
            logger = logger,
            mapError = { AppError.Server(requestId = "req-1") },
        ) {
            throw IllegalStateException("boom")
        }

        assertEquals(Outcome.Failure(AppError.Server("req-1")), result)
    }

    @Test
    fun `safeOutcome rethrows cancellation without logging`() {
        assertThrows(CancellationException::class.java) {
            runBlocking {
                safeOutcome("test", "op", logger) { throw CancellationException("cancelled") }
            }
        }
        assertTrue(sink.events.isEmpty())
    }

    private class RecordingSink : LogSink {
        val events = mutableListOf<LogEvent>()

        override fun write(event: LogEvent) {
            events += event
        }
    }
}
