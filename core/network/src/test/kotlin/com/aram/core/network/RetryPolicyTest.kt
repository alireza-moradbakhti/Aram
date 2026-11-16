package com.aram.core.network

import com.aram.core.network.api.RetryPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RetryPolicyTest {

    @Test
    fun `backoff stays inside the configured window`() {
        val policy = RetryPolicy(maxAttempts = 3, minBackoffMs = 200, maxBackoffMs = 600, random = Random(7))
        repeat(50) { index ->
            val delay = policy.backoffMs(index)
            assertTrue("attempt $index produced $delay", delay in 200..600)
        }
    }

    @Test
    fun `none allows a single attempt`() {
        assertEquals(1, RetryPolicy.NONE.maxAttempts)
        assertEquals(2, RetryPolicy.DEFAULT.maxAttempts)
    }

    @Test
    fun `invalid budgets are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { RetryPolicy(maxAttempts = 0) }
        assertThrows(IllegalArgumentException::class.java) {
            RetryPolicy(minBackoffMs = 500, maxBackoffMs = 100)
        }
    }
}
