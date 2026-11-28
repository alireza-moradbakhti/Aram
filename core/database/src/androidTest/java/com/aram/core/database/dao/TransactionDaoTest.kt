package com.aram.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aram.core.database.AramDatabase
import com.aram.core.database.entity.TransactionEntity
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented coverage for the queries behind the transactions list, totals
 * and sync tombstones. Every test gets a fresh in-memory database.
 */
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var db: AramDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AramDatabase::class.java).build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun monthRangeIncludesItsFirstInstantAndExcludesTheNextMonth() = runBlocking {
        insert(
            transaction("sep", occurredAt = SEP_30_LAST_MS),
            transaction("first", occurredAt = OCT_START),
            transaction("last", occurredAt = OCT_END),
            transaction("next", occurredAt = NOV_START),
        )

        val rows = dao.observeRange(USER, OCT_START, NOV_START).first()

        assertEquals(listOf("last", "first"), rows.map { it.id })
    }

    @Test
    fun totalUsesTheSameHalfOpenMonthRange() = runBlocking {
        insert(
            transaction("first", amountMinor = 1_000, occurredAt = OCT_START),
            transaction("last", amountMinor = 2_000, occurredAt = OCT_END),
            transaction("next", amountMinor = 4_000, occurredAt = NOV_START),
            transaction("before", amountMinor = 8_000, occurredAt = SEP_30_LAST_MS),
        )

        val total = dao.observeTotal(USER, TYPE_EXPENSE, OCT_START, NOV_START).first()

        assertEquals(3_000, total)
    }

    @Test
    fun softDeletedRowsDisappearFromEveryRead() = runBlocking {
        insert(
            transaction("kept", amountMinor = 1_000, occurredAt = OCT_START),
            transaction("gone", amountMinor = 2_000, occurredAt = OCT_START + 1),
        )

        assertEquals(1, dao.softDelete(USER, "gone", deletedAt = 10))
        assertEquals(0, dao.softDelete(USER, "gone", deletedAt = 11))

        assertEquals(listOf("kept"), dao.observeRange(USER, OCT_START, NOV_START).first().map { it.id })
        assertEquals(listOf("kept"), dao.observeRecent(USER, limit = 10).first().map { it.id })
        assertEquals(1_000, dao.observeTotal(USER, TYPE_EXPENSE, OCT_START, NOV_START).first())
        assertNull(dao.findById(USER, "gone"))
        assertNull(dao.observeById(USER, "gone").first())
    }

    @Test
    fun incomeAndExpenseTotalsAreReportedSeparately() = runBlocking {
        insert(
            transaction("e1", amountMinor = 3_000, occurredAt = OCT_START),
            transaction("e2", amountMinor = 2_000, occurredAt = OCT_START + 1),
            transaction("i1", type = TYPE_INCOME, amountMinor = 9_000, occurredAt = OCT_START),
            transaction(
                "deleted",
                amountMinor = 7_000,
                occurredAt = OCT_START,
                deletedAt = 1,
            ),
        )

        assertEquals(5_000, dao.observeTotal(USER, TYPE_EXPENSE, OCT_START, NOV_START).first())
        assertEquals(9_000, dao.observeTotal(USER, TYPE_INCOME, OCT_START, NOV_START).first())
    }

    @Test
    fun recentReturnsNewestLiveRowsUpToTheLimit() = runBlocking {
        insert(
            transaction("oldest", occurredAt = OCT_START),
            transaction("middle", occurredAt = OCT_START + 2),
            transaction("newest", occurredAt = OCT_START + 4),
            transaction("deleted-newest", occurredAt = OCT_START + 6, deletedAt = 1),
        )

        val recent = dao.observeRecent(USER, limit = 2).first()

        assertEquals(listOf("newest", "middle"), recent.map { it.id })
    }

    @Test
    fun softDeleteMissingTombstonesAbsentRowsButSparesQueuedPushes() = runBlocking {
        insert(
            transaction("pulled-away", occurredAt = OCT_START),
            transaction("queued", occurredAt = OCT_START + 1, pendingPush = true),
        )

        val tombstoned = dao.softDeleteMissing(
            userId = USER,
            fromInclusive = OCT_START,
            toExclusive = NOV_START,
            keepIds = emptyList(),
            deletedAt = 10,
        )

        assertEquals(1, tombstoned)
        val pulledAway = dao.findByIds(USER, listOf("pulled-away")).single()
        assertEquals(10L, pulledAway.deletedAt)
        assertEquals(10L, pulledAway.updatedAt)
        assertNull(dao.findById(USER, "queued")?.deletedAt)
    }

    @Test
    fun softDeleteAllInRangeClearsTheWindowExceptQueuedPushes() = runBlocking {
        insert(
            transaction("plain", occurredAt = OCT_START),
            transaction("queued", occurredAt = OCT_START + 1, pendingPush = true),
            transaction("outside", occurredAt = NOV_START),
        )

        val cleared = dao.softDeleteAllInRange(USER, OCT_START, NOV_START, deletedAt = 20)

        assertEquals(1, cleared)
        assertNull(dao.observeById(USER, "plain").first())
        assertNotNull(dao.observeById(USER, "queued").first())
        assertNotNull(dao.observeById(USER, "outside").first())
    }

    private suspend fun insert(vararg rows: TransactionEntity) = dao.upsertAll(rows.toList())

    private fun transaction(
        id: String,
        type: String = TYPE_EXPENSE,
        amountMinor: Long = 1_000,
        occurredAt: Long,
        deletedAt: Long? = null,
        pendingPush: Boolean = false,
    ) = TransactionEntity(
        id = id,
        userId = USER,
        type = type,
        accountId = "account-1",
        categoryId = "category-1",
        amountMinor = amountMinor,
        currencyCode = "IRR",
        occurredAt = occurredAt,
        note = null,
        transferGroupId = null,
        recurringRuleId = null,
        createdAt = 0,
        updatedAt = 0,
        deletedAt = deletedAt,
        pendingPush = pendingPush,
    )

    private companion object {
        const val USER = "user-1"
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"

        val SEP_30_LAST_MS = Instant.parse("2026-09-30T23:59:59.999Z").toEpochMilli()
        val OCT_START = Instant.parse("2026-10-01T00:00:00Z").toEpochMilli()
        val OCT_END = Instant.parse("2026-10-31T23:59:59.999Z").toEpochMilli()
        val NOV_START = Instant.parse("2026-11-01T00:00:00Z").toEpochMilli()
    }
}