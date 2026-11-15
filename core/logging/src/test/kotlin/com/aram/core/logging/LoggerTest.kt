package com.aram.core.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggerTest {

    private val local = mutableListOf<LogEvent>()
    private val remote = mutableListOf<LogEvent>()

    private fun logger(
        minLevel: LogLevel = LogLevel.VERBOSE,
        serverUploadEnabled: Boolean = false,
    ) = Logger(
        config = LogConfig(minLevel = minLevel, serverUploadEnabled = serverUploadEnabled),
        sinks = listOf(LogSink { local += it }),
        remoteSink = RemoteLogSink { remote += it },
    )

    @Test
    fun `logs stay local by default`() {
        logger(serverUploadEnabled = true).d("Repo", "loaded 3 rows")

        assertEquals(1, local.size)
        assertEquals(false, local.single().sendToServer)
        assertTrue(remote.isEmpty())
    }

    @Test
    fun `only flagged events reach the server sink`() {
        val logger = logger(serverUploadEnabled = true)

        logger.d("Repo", "local detail")
        logger.e("Repo", "sync failed", sendToServer = true)

        assertEquals(2, local.size)
        assertEquals(1, remote.size)
        assertEquals("sync failed", remote.single().message)
    }

    @Test
    fun `global switch off keeps flagged events local`() {
        logger(serverUploadEnabled = false).e("Repo", "sync failed", sendToServer = true)

        assertEquals(1, local.size)
        assertTrue(remote.isEmpty())
    }

    @Test
    fun `events below the minimum level are dropped`() {
        val logger = logger(minLevel = LogLevel.WARN)

        logger.d("Repo", "debug detail")
        logger.w("Repo", "slow query")

        assertEquals(1, local.size)
        assertEquals(LogLevel.WARN, local.single().level)
    }

    @Test
    fun `name, message, and throwable are preserved`() {
        val failure = IllegalStateException("boom")

        logger().e("TransactionsRepository", "insert failed", failure)

        val event = local.single()
        assertEquals("TransactionsRepository", event.name)
        assertEquals("insert failed", event.message)
        assertEquals(failure, event.throwable)
        assertNull(event.throwable?.cause)
    }
}
