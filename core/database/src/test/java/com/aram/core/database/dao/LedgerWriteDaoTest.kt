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
    fun `transfer legs are written together without an outbox entry`() = runBlocking {
        ledger.createTransfer(listOf(leg("out", -50_000L), leg("in", 50_000L)))

        val dao = db.transactionDao()
        assertEquals(-50_000L, dao.findById(USER, "out")?.amountMinor)
        assertEquals(50_000L, dao.findById(USER, "in")?.amountMinor)
        assertEquals("TRANSFER", dao.findById(USER, "in")?.type)
        assertEquals(0, outbox.count(USER))
    }

    @Test
    fun `a failing leg rolls the whole transfer back`() = runBlocking {
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_in BEFORE INSERT ON transactions " +
                "WHEN NEW.id = 'in' BEGIN SELECT RAISE(ABORT, 'rejected'); END",
        )

        var failure: Throwable? = null
        try {
            ledger.createTransfer(listOf(leg("out", -50_000L), leg("in", 50_000L)))
        } catch (e: Throwable) {
            failure = e
        }

        assertNotNull(failure)
        assertNull(db.transactionDao().findById(USER, "out"))
        assertNull(db.transactionDao().findById(USER, "in"))
        assertEquals(0, outbox.count(USER))
    }

    @Test
    fun `deleting a transfer group tombstones every leg without queuing`() = runBlocking {
        ledger.createTransfer(listOf(leg("out", -50_000L), leg("in", 50_000L)))

        val changed = ledger.deleteTransferGroup(USER, GROUP, deletedAt = 10L)

        assertEquals(2, changed)
        assertNull(db.transactionDao().findById(USER, "out"))
        assertNull(db.transactionDao().findById(USER, "in"))
        assertEquals(0, outbox.count(USER))
    }

    @Test
    fun `the month sweep spares transfer legs`() = runBlocking {
        db.transactionDao().upsert(transaction("t1"))
        ledger.createTransfer(listOf(leg("out", -50_000L), leg("in", 50_000L)))

        val swept = db.transactionDao().softDeleteMissing(
            userId = USER,
            fromInclusive = 0L,
            toExclusive = 10_000L,
            keepIds = emptyList(),
            deletedAt = 10L,
        )

        assertEquals(1, swept)
        assertNull(db.transactionDao().findById(USER, "t1"))
        assertNotNull(db.transactionDao().findById(USER, "out"))
        assertNotNull(db.transactionDao().findById(USER, "in"))
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

    private fun leg(id: String, amountMinor: Long) = TransactionEntity(
        id = id,
        userId = USER,
        type = "TRANSFER",
        accountId = if (amountMinor < 0) "a1" else "a2",
        categoryId = null,
        amountMinor = amountMinor,
        currencyCode = "IRR",
        occurredAt = 0L,
        note = null,
        transferGroupId = GROUP,
        recurringRuleId = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
        pendingPush = false,
    )

    private companion object {
        const val USER = "user-1"
        const val GROUP = "group-1"
    }
}
