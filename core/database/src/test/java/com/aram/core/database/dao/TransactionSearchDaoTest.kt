package com.aram.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aram.core.database.AramDatabase
import com.aram.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionSearchDaoTest {

    private lateinit var db: AramDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AramDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `note search is case-insensitive and partial`() = runBlocking {
        insert(row("a", note = "Coffee with Nima"))
        insert(row("b", note = "Lunch"))
        insert(row("c", note = null))

        assertEquals(listOf("a"), search(noteQuery = "cOfFeE").map { it.id })
        assertEquals(listOf("a", "b"), search(noteQuery = "n").map { it.id }.sorted())
    }

    @Test
    fun `deleted rows never match`() = runBlocking {
        insert(row("gone", note = "coffee", deletedAt = 10L))
        insert(row("kept", note = "coffee"))

        assertEquals(listOf("kept"), search(noteQuery = "coffee").map { it.id })
    }

    @Test
    fun `type keeps only that type`() = runBlocking {
        insert(row("expense", type = "EXPENSE"))
        insert(row("income", type = "INCOME"))

        assertEquals(listOf("income"), search(type = "INCOME").map { it.id })
    }

    @Test
    fun `exclude type drops income but keeps transfers`() = runBlocking {
        insert(row("expense", type = "EXPENSE"))
        insert(row("income", type = "INCOME"))
        insert(row("transfer", type = "TRANSFER"))

        assertEquals(
            listOf("expense", "transfer"),
            search(excludeType = "INCOME").map { it.id }.sorted(),
        )
    }

    @Test
    fun `category matches exactly`() = runBlocking {
        insert(row("food", categoryId = "c-food"))
        insert(row("ride", categoryId = "c-ride"))

        assertEquals(listOf("food"), search(categoryId = "c-food").map { it.id })
    }

    @Test
    fun `date bounds are half open`() = runBlocking {
        insert(row("before", occurredAt = 999L))
        insert(row("first", occurredAt = 1_000L))
        insert(row("last", occurredAt = 1_999L))
        insert(row("after", occurredAt = 2_000L))

        val ids = search(fromInclusive = 1_000L, toExclusive = 2_000L).map { it.id }.sorted()

        assertEquals(listOf("first", "last"), ids)
    }

    @Test
    fun `results are newest first`() = runBlocking {
        insert(row("old", occurredAt = 1_000L))
        insert(row("new", occurredAt = 3_000L))
        insert(row("mid", occurredAt = 2_000L))

        assertEquals(listOf("new", "mid", "old"), search().map { it.id })
    }

    @Test
    fun `every filter composes`() = runBlocking {
        insert(
            row("match", type = "EXPENSE", categoryId = "c-food", occurredAt = 1_500L, note = "Coffee"),
        )
        insert(row("otherType", type = "INCOME", categoryId = "c-food", occurredAt = 1_500L, note = "Coffee"))
        insert(row("otherCategory", type = "EXPENSE", categoryId = "c-ride", occurredAt = 1_500L, note = "Coffee"))
        insert(row("outside", type = "EXPENSE", categoryId = "c-food", occurredAt = 5_000L, note = "Coffee"))
        insert(row("otherNote", type = "EXPENSE", categoryId = "c-food", occurredAt = 1_500L, note = "Tea"))

        val ids = search(
            noteQuery = "coffee",
            type = "EXPENSE",
            excludeType = "INCOME",
            categoryId = "c-food",
            fromInclusive = 1_000L,
            toExclusive = 2_000L,
        ).map { it.id }

        assertEquals(listOf("match"), ids)
    }

    @Test
    fun `no matches emits an empty list`() = runBlocking {
        insert(row("a", note = "Coffee"))

        assertEquals(emptyList<String>(), search(noteQuery = "taxi").map { it.id })
    }

    @Test
    fun `rows of another user are invisible`() = runBlocking {
        insert(row("mine", note = "coffee", userId = USER))
        insert(row("theirs", note = "coffee", userId = "user-2"))

        assertEquals(listOf("mine"), search(noteQuery = "coffee").map { it.id })
    }

    private suspend fun insert(entity: TransactionEntity) {
        db.transactionDao().upsert(entity)
    }

    private suspend fun search(
        noteQuery: String = "",
        type: String? = null,
        excludeType: String? = null,
        categoryId: String? = null,
        fromInclusive: Long? = null,
        toExclusive: Long? = null,
    ): List<TransactionEntity> = db.transactionDao().search(
        userId = USER,
        noteQuery = noteQuery,
        type = type,
        excludeType = excludeType,
        categoryId = categoryId,
        fromInclusive = fromInclusive,
        toExclusive = toExclusive,
    ).first()

    private fun row(
        id: String,
        type: String = "EXPENSE",
        note: String? = null,
        categoryId: String? = "c-food",
        occurredAt: Long = 1_000L,
        userId: String = USER,
        deletedAt: Long? = null,
    ) = TransactionEntity(
        id = id,
        userId = userId,
        type = type,
        accountId = "acc",
        categoryId = categoryId,
        amountMinor = 1_000L,
        currencyCode = "IRR",
        occurredAt = occurredAt,
        note = note,
        transferGroupId = null,
        recurringRuleId = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = deletedAt,
    )

    private companion object {
        const val USER = "user-1"
    }
}