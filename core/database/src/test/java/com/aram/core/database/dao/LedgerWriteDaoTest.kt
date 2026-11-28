package com.aram.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aram.core.database.AramDatabase
import com.aram.core.database.entity.SyncOutboxEntity
import com.aram.core.database.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LedgerWriteDaoTest {

    private lateinit var db: AramDatabase
    private lateinit var ledger: LedgerWriteDao
    private lateinit var outbox: SyncOutboxDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AramDatabase::class.java).build()
        ledger = db.ledgerWriteDao()
        outbox = db.syncOutboxDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `create writes the row and its outbox entry together`() = runBlocking {
        ledger.createWithOutbox(transaction("t1"), entry("e1", "t1", SyncOutboxEntity.OP_CREATE))

        assertNotNull(db.transactionDao().findById(USER, "t1"))
        assertEquals("t1", outbox.due(USER, now = 0, limit = 10).single().entityId)
    }

    @Test
    fun `updating a missing row queues nothing`() = runBlocking {
        val changed = ledger.updateWithOutbox(
            userId = USER,
            id = "missing",
            accountId = "a1",
            categoryId = null,
            type = "EXPENSE",
            amountMinor = 2_000L,
            currencyCode = "IRR",
            occurredAt = 0L,
            note = "edited",
            updatedAt = 10L,
            entry = entry("e1", "missing", SyncOutboxEntity.OP_UPDATE),
        )

        assertEquals(0, changed)
        assertEquals(0, outbox.count(USER))
    }

    @Test
    fun `updating an existing row flips the queued flag and enqueues the push`() = runBlocking {
        db.transactionDao().upsert(transaction("t1"))

        val changed = ledger.updateWithOutbox(
            userId = USER,
            id = "t1",
            accountId = "a2",
            categoryId = "c1",
            type = "EXPENSE",
            amountMinor = 2_000L,
            currencyCode = "IRR",
            occurredAt = 5L,
            note = "edited",
            updatedAt = 10L,
            entry = entry("e1", "t1", SyncOutboxEntity.OP_UPDATE),
        )

        assertEquals(1, changed)
        val row = db.transactionDao().findById(USER, "t1")
        assertEquals("edited", row?.note)
        assertEquals(true, row?.pendingPush)
        assertEquals(SyncOutboxEntity.OP_UPDATE, outbox.due(USER, now = 0, limit = 10).single().op)
    }

    @Test
    fun `deleting soft-deletes the row and enqueues the delete`() = runBlocking {
        db.transactionDao().upsert(transaction("t1"))

        val changed = ledger.deleteWithOutbox(
            userId = USER,
            id = "t1",
            deletedAt = 10L,
            entry = entry("e1", "t1", SyncOutboxEntity.OP_DELETE),
        )

        assertEquals(1, changed)
        assertNull(db.transactionDao().findById(USER, "t1"))
        assertEquals(SyncOutboxEntity.OP_DELETE, outbox.due(USER, now = 0, limit = 10).single().op)
    }

    @Test
    fun `a row cannot be deleted twice`() = runBlocking {
        db.transactionDao().upsert(transaction("t1"))

        ledger.deleteWithOutbox(USER, "t1", 10L, entry("e1", "t1", SyncOutboxEntity.OP_DELETE))
        val second = ledger.deleteWithOutbox(USER, "t1", 20L, entry("e2", "t1", SyncOutboxEntity.OP_DELETE))

        assertEquals(0, second)
        assertEquals(1, outbox.count(USER))
    }

    @Test
    fun `restoring clears the tombstone and enqueues the update`() = runBlocking {
        db.transactionDao().upsert(transaction("t1"))
        ledger.deleteWithOutbox(USER, "t1", 10L, entry("e1", "t1", SyncOutboxEntity.OP_DELETE))

        val changed = ledger.restoreWithOutbox(
            userId = USER,
            id = "t1",
            updatedAt = 20L,
            entry = entry("e2", "t1", SyncOutboxEntity.OP_UPDATE),
        )

        assertEquals(1, changed)
        val row = db.transactionDao().findById(USER, "t1")
        assertEquals(20L, row?.updatedAt)
        assertEquals(true, row?.pendingPush)
        assertEquals(2, outbox.count(USER))
    }

    @Test
    fun `restoring a live row queues nothing`() = runBlocking {
        db.transactionDao().upsert(transaction("t1"))

        val changed = ledger.restoreWithOutbox(
            userId = USER,
            id = "t1",
            updatedAt = 20L,
            entry = entry("e1", "t1", SyncOutboxEntity.OP_UPDATE),
        )

        assertEquals(0, changed)
        assertEquals(0, outbox.count(USER))
    }

    private fun entry(id: String, entityId: String, op: String) = SyncOutboxEntity(
        id = id,
        userId = USER,
        entityId = entityId,
        op = op,
        payload = null,
        createdAt = 1L,
    )

    private fun transaction(id: String) = TransactionEntity(
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
    )

    private companion object {
        const val USER = "user-1"
    }
}
