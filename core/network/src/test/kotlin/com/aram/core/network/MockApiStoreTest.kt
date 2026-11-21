package com.aram.core.network

import com.aram.core.network.dto.ContributeGoalRequest
import com.aram.core.network.dto.CreateGoalRequest
import com.aram.core.network.dto.LoginRequest
import com.aram.core.network.dto.RegisterRequest
import com.aram.core.network.dto.SaveBudgetRequest
import com.aram.core.network.dto.SaveTransactionRequest
import com.aram.core.network.dto.SyncPushRequest
import com.aram.core.network.dto.SyncTransactionItem
import com.aram.core.network.mock.MockApiStore
import com.aram.core.network.mock.MockDemoAccount
import com.aram.core.network.mock.MockOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MockApiStoreTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC)

    private fun store() = MockApiStore(clock = clock)

    private fun <T> MockOutcome<T>.value(): T = (this as MockOutcome.Ok<T>).data

    private fun <T> MockOutcome<T>.error(): MockOutcome.Error = this as MockOutcome.Error

    private fun demoToken(store: MockApiStore): String =
        store.login(LoginRequest(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD)).value().token

    private fun newUserToken(store: MockApiStore, email: String = "new@aram.app"): String =
        store.register(RegisterRequest(email = email, password = "secret123", displayName = "New User"))
            .value()
            .token

    @Test
    fun `demo account signs in with seeded data`() {
        val store = store()
        val token = demoToken(store)

        assertEquals("Ava Bennett", store.me(token).value().displayName)
        assertEquals(10, store.categories(null).value().size)
        assertEquals(3, store.accounts(token).value().size)
        assertEquals(3, store.budgets(token).value().size)
        assertEquals(2, store.goals(token, includeArchived = false).value().size)

        val november = store.listTransactions(token, 2026, 11, null, 1, 50, null).value()
        assertEquals(12, november.totalCount)
        assertTrue(november.groups.isNotEmpty())
    }

    @Test
    fun `bad credentials and bad tokens are rejected`() {
        val store = store()
        assertEquals(401, store.login(LoginRequest(MockDemoAccount.EMAIL, "wrong-pass")).error().status)
        assertEquals(401, store.me("not-a-token").error().status)
        assertEquals(401, store.transactionSummary("not-a-token", null, null).error().status)
    }

    @Test
    fun `registration validates fields and rejects duplicates`() {
        val store = store()
        val invalid = store.register(
            RegisterRequest(email = "bad", password = "short", displayName = " "),
        ).error()
        assertEquals(400, invalid.status)
        assertEquals(setOf("email", "password", "displayName"), invalid.fields.keys)

        val duplicate = store.register(
            RegisterRequest(email = MockDemoAccount.EMAIL, password = "secret123", displayName = "Dup"),
        ).error()
        assertEquals(400, duplicate.status)
        assertEquals(listOf("Already registered."), duplicate.fields["email"])
    }

    @Test
    fun `ledgers are isolated per user`() {
        val store = store()
        val demo = demoToken(store)
        val fresh = newUserToken(store)

        assertEquals(0, store.listTransactions(fresh, 2026, 11, null, 1, 50, null).value().totalCount)

        val created = store.createTransaction(
            fresh,
            SaveTransactionRequest(kind = "expense", amount = 250_000.0, categorySlug = "groceries"),
        ).value()
        assertEquals(1, store.listTransactions(fresh, 2026, 11, null, 1, 50, null).value().totalCount)
        assertEquals(12, store.listTransactions(demo, 2026, 11, null, 1, 50, null).value().totalCount)

        val updated = store.updateTransaction(
            fresh,
            created.id,
            SaveTransactionRequest(kind = "expense", amount = 300_000.0, categorySlug = "groceries"),
        ).value()
        assertEquals(300_000.0, updated.amountIrr, 0.001)
        assertTrue(store.deleteTransaction(fresh, created.id).value())
        assertEquals(404, store.deleteTransaction(fresh, created.id).error().status)
    }

    @Test
    fun `entered currency converts to irr`() {
        val store = store()
        val token = demoToken(store)
        val tx = store.createTransaction(
            token,
            SaveTransactionRequest(
                kind = "expense",
                amount = 10.0,
                currency = "USD",
                categorySlug = "dining",
                note = "Coffee abroad",
            ),
        ).value()

        assertEquals(10.0, requireNotNull(tx.enteredAmount), 0.001)
        assertEquals("USD", tx.enteredCurrency)
        assertEquals(10_420_000.0, tx.amountIrr, 0.001)
    }

    @Test
    fun `expense kind must match the category kind`() {
        val store = store()
        val token = demoToken(store)
        val error = store.createTransaction(
            token,
            SaveTransactionRequest(kind = "income", amount = 1_000.0, categorySlug = "groceries"),
        ).error()
        assertEquals(400, error.status)
    }

    @Test
    fun `budgets reject duplicates and edit affects the overview`() {
        val store = store()
        val token = demoToken(store)

        val duplicate = store.createBudget(
            token,
            SaveBudgetRequest(categorySlug = "groceries", monthlyLimitIrr = 1_000_000.0),
        ).error()
        assertEquals(409, duplicate.status)

        val created = store.createBudget(
            token,
            SaveBudgetRequest(categorySlug = "shopping", monthlyLimitIrr = 2_000_000.0),
        ).value()
        store.updateBudget(token, created.id, com.aram.core.network.dto.UpdateBudgetRequest(3_000_000.0))

        val overview = store.budgetOverview(token, 2026, 11).value()
        assertTrue(overview.categories.any { it.id == created.id && it.limitIrr == 3_000_000.0 })
    }

    @Test
    fun `goal contributions accumulate`() {
        val store = store()
        val token = newUserToken(store)
        val goal = store.createGoal(
            token,
            CreateGoalRequest(name = "Laptop", targetIrr = 60_000_000.0, savedIrr = 5_000_000.0),
        ).value()
        assertEquals(5_000_000.0, goal.savedIrr, 0.001)

        val after = store.contribute(token, goal.id, ContributeGoalRequest(amount = 10_000_000.0)).value()
        assertEquals(15_000_000.0, after.savedIrr, 0.001)
        assertEquals(45_000_000.0, after.remainingIrr, 0.001)
    }

    @Test
    fun `sync push applies new rows and keeps the newest edit`() {
        val store = store()
        val token = demoToken(store)
        val id = "offline-1"

        val first = store.syncPush(
            token,
            SyncPushRequest(
                items = listOf(
                    SyncTransactionItem(
                        id = id,
                        kind = "expense",
                        amountIrr = 400_000.0,
                        categorySlug = "fun",
                        occurredAt = "2026-11-20T10:00:00Z",
                        clientUpdatedAt = "2026-11-20T10:00:00Z",
                    ),
                ),
            ),
        ).value()
        assertEquals(listOf(id), first.applied)
        assertEquals(emptyList<String>(), first.skipped)

        val stale = store.syncPush(
            token,
            SyncPushRequest(
                items = listOf(
                    SyncTransactionItem(
                        id = id,
                        kind = "expense",
                        amountIrr = 100_000.0,
                        categorySlug = "fun",
                        clientUpdatedAt = "2026-11-19T10:00:00Z",
                    ),
                ),
            ),
        ).value()
        assertEquals(emptyList<String>(), stale.applied)
        assertEquals(listOf(id), stale.skipped)

        val newer = store.syncPush(
            token,
            SyncPushRequest(
                items = listOf(
                    SyncTransactionItem(
                        id = id,
                        kind = "expense",
                        amountIrr = 700_000.0,
                        categorySlug = "fun",
                        occurredAt = "2026-11-20T10:00:00Z",
                        clientUpdatedAt = "2026-11-21T10:00:00Z",
                    ),
                ),
            ),
        ).value()
        assertEquals(listOf(id), newer.applied)

        val pulled = store.syncPull(token, since = null).value()
        val row = pulled.transactions.single { it.id == id }
        assertEquals(700_000.0, row.amountIrr, 0.001)
        assertTrue(pulled.settings != null)
    }

    @Test
    fun `sync pull honours the since stamp`() {
        val store = store()
        val token = demoToken(store)
        assertTrue(store.syncPull(token, since = null).value().transactions.isNotEmpty())
        assertEquals(0, store.syncPull(token, since = "2100-01-01T00:00:00Z").value().transactions.size)
    }
}
