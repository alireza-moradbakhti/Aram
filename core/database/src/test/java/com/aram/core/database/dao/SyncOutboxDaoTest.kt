package com.aram.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aram.core.database.AramDatabase
import com.aram.core.database.entity.SyncOutboxEntity
import com.aram.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SyncOutboxDaoTest {

    private lateinit var db: AramDatabase
    private lateinit var outbox: SyncOutboxDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AramDatabase::class.java).build()
        outbox = db.syncOutboxDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `due returns the oldest entries first`() = runBlocking {
        outbox.insert(entry("c", createdAt = 3))
        outbox.insert(entry("a", createdAt = 1))
        outbox.insert(entry("b", createdAt = 2))

        assertEquals(listOf("a", "b", "c"), outbox.due(USER, now = 100, limit = 10).map { it.id })
    }

    @Test
    fun `due skips parked entries and other users`() = runBlocking {
        outbox.insert(entry("due", createdAt = 1))
        outbox.insert(entry("parked", createdAt = 2, nextAttemptAt = 500))
        outbox.insert(entry("other", createdAt = 3, userId = "user-2"))

        assertEquals(listOf("due"), outbox.due(USER, now = 100, limit = 10).map { it.id })
    }

    @Test
    fun `due honours the batch limit`() = runBlocking {
        repeat(5) { index -> outbox.insert(entry("e$index", createdAt = index.toLong())) }

        assertEquals(2, outbox.due(USER, now = 100, limit = 2).size)
    }

    @Test
    fun `markAttempt parks the entry with its error`() = runBlocking {
        outbox.insert(entry("e", createdAt = 1))

        outbox.markAttempt("e", attempts = 3, nextAttemptAt = 5_000, lastError = "timeout")

        assertTrue(outbox.due(USER, now = 4_999, limit = 10).isEmpty())
        val parked = outbox.due(USER, now = 5_000, limit = 10).single()
        assertEquals(3, parked.attempts)
        assertEquals(5_000, parked.nextAttemptAt)
        assertEquals("timeout", parked.lastError)
    }

    @Test
    fun `retryAll makes failed entries due immediately`() = runBlocking {
        outbox.insert(entry("e", createdAt = 1))
        outbox.markAttempt("e", attempts = 4, nextAttemptAt = 5_000, lastError = "timeout")

        outbox.retryAll(USER)

        val retried = outbox.due(USER, now = 0, limit = 10).single()
        assertEquals(0, retried.attempts)
        assertEquals(0, retried.nextAttemptAt)
    }

    @Test
    fun `countFailed counts attempts at or above the threshold`() = runBlocking {
        outbox.insert(entry("one", createdAt = 1, attempts = 1))
        outbox.insert(entry("three", createdAt = 2, attempts = 3))
        outbox.insert(entry("five", createdAt = 3, attempts = 5))
        outbox.insert(entry("other", createdAt = 4, attempts = 9, userId = "user-2"))

        assertEquals(2, outbox.countFailed(USER, attempts = 3))
        assertEquals(2, outbox.observeFailed(USER, attempts = 3).first())
    }

    @Test
    fun `markSynced clears the queued chip on the ledger row`() = runBlocking {
        db.transactionDao().upsert(transaction("t1", pendingPush = true))
        outbox.insert(entry("e", entityId = "t1", createdAt = 1))

        outbox.markSynced(USER, "t1")

        assertEquals(false, db.transactionDao().findById(USER, "t1")?.pendingPush)
    }

    @Test
    fun `discard drops only the queued work for one entity`() = runBlocking {
        outbox.insert(entry("e1", entityId = "t1", createdAt = 1))
        outbox.insert(entry("e2", entityId = "t2", createdAt = 2))
        outbox.insert(entry("e3", entityId = "t2", createdAt = 3))

        outbox.discard(USER, "t2")

        assertEquals(1, outbox.count(USER))
        assertEquals("t1", outbox.due(USER, now = 100, limit = 10).single().entityId)
    }

    @Test
    fun `deleteByUser leaves other users alone`() = runBlocking {
        outbox.insert(entry("a", createdAt = 1))
        outbox.insert(entry("b", createdAt = 2, userId = "user-2"))

        outbox.deleteByUser(USER)

        assertEquals(0, outbox.count(USER))
        assertEquals(1, outbox.count("user-2"))
    }

    @Test
    fun `observeCount follows inserts and deletes`() = runBlocking {
        assertEquals(0, outbox.observeCount(USER).first())

        outbox.insert(entry("e", createdAt = 1))
        assertEquals(1, outbox.observeCount(USER).first())

        outbox.delete("e")
        assertEquals(0, outbox.observeCount(USER).first())
    }

    private fun entry(
        id: String,
        entityId: String = id,
        userId: String = USER,
        createdAt: Long,
        nextAttemptAt: Long = 0L,
        attempts: Int = 0,
    ) = SyncOutboxEntity(
        id = id,
        userId = userId,
        entityId = entityId,
        op = SyncOutboxEntity.OP_CREATE,
        payload = """{"id":"$entityId"}""",
        attempts = attempts,
        nextAttemptAt = nextAttemptAt,
        createdAt = createdAt,
    )

    private fun transaction(id: String, pendingPush: Boolean) = TransactionEntity(
        id = id,
        userId = USER,
        type = "EXPENSE",
        accountId = "a1",
        categoryId = null,
        amountMinor = 1_000L,
        currencyCode = "IRR",
        occurredAt = 0L,
        note = null,
        transferGroupId = null,
        recurringRuleId = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
        pendingPush = pendingPush,
    )

    private companion object {
        const val USER = "user-1"
    }
}
