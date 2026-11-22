package com.aram.core.data

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.data.http.SessionTokenProvider
import com.aram.core.data.repository.AuthRepositoryImpl
import com.aram.core.database.entity.BudgetEntity
import com.aram.core.database.entity.RecurringRuleEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.database.entity.UserEntity
import com.aram.core.datastore.SessionToken
import com.aram.core.logging.Logger
import com.aram.core.model.AppError
import com.aram.core.model.CalendarSystem
import com.aram.core.model.Outcome
import com.aram.core.model.UserProfile
import com.aram.core.network.config.ApiConfig
import com.aram.core.network.config.ApiEnvironment
import com.aram.core.network.config.ApiMode
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.http.AramHttpClient
import com.aram.core.network.mock.MockApiEngine
import com.aram.core.network.mock.MockApiStore
import com.aram.core.network.mock.MockDemoAccount
import com.aram.core.network.mock.MockScenario
import com.aram.core.network.mock.MockScenarioController
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    @Test
    fun `register starts the session, stores the token and seeds defaults`() = runTest {
        val harness = harness(UnconfinedTestDispatcher(testScheduler))

        val outcome = harness.repository.register(
            displayName = "Aram Moradi",
            email = "aram@example.com",
            password = "aram1234",
        )

        val profile = (outcome as Outcome.Success).value
        assertEquals("Aram Moradi", profile.displayName)
        assertEquals("IRR", profile.defaultCurrency)
        assertEquals(profile.id, harness.sessionStore.value)
        val token = harness.tokenStore.read()
        assertNotNull(token)
        assertTrue(token!!.value.isNotBlank())
        assertEquals(harness.clock.millis() + DAY_MILLIS, token.expiresAtEpochMillis)
        assertEquals("Aram Moradi", harness.users.rows[profile.id]?.displayName)
        assertEquals(12, harness.categories.rows.values.count { it.userId == profile.id })
        assertEquals(1, harness.accounts.rows.values.count { it.userId == profile.id })
    }

    @Test
    fun `register rejects weak input before touching the network`() = runTest {
        val harness = harness(UnconfinedTestDispatcher(testScheduler))

        val outcome = harness.repository.register("Aram", "not-an-email", "short")

        val error = (outcome as Outcome.Failure).error as AppError.Validation
        assertEquals(setOf("email", "password"), error.fields.keys)
        assertNull(harness.sessionStore.value)
        assertNull(harness.tokenStore.read())
    }

    @Test
    fun `register surfaces a duplicate email as a field error`() = runTest {
        val harness = harness(UnconfinedTestDispatcher(testScheduler))

        val outcome = harness.repository.register(
            displayName = "Ava Again",
            email = MockDemoAccount.EMAIL,
            password = MockDemoAccount.PASSWORD,
        )

        val error = (outcome as Outcome.Failure).error as AppError.Validation
        assertTrue(error.fields.containsKey("email"))
        assertNull(harness.sessionStore.value)
    }

    @Test
    fun `sign in with a wrong password is unauthorized`() = runTest {
        val harness = harness(UnconfinedTestDispatcher(testScheduler))

        val outcome = harness.repository.signIn(MockDemoAccount.EMAIL, "wrong-password")

        assertEquals(Outcome.Failure(AppError.Unauthorized), outcome)
        assertNull(harness.sessionStore.value)
        assertNull(harness.tokenStore.read())
    }

    @Test
    fun `sign in seeds the cache once and stores a session`() = runTest {
        val harness = harness(UnconfinedTestDispatcher(testScheduler))

        val first = (harness.repository.signIn(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD) as Outcome.Success).value
        harness.repository.signIn(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD)

        assertEquals("Ava Bennett", first.displayName)
        assertEquals(1, harness.categories.upsertBatches)
        assertEquals(first.id, harness.sessionStore.value)
        assertNotNull(harness.tokenStore.read())
    }

    @Test
    fun `guest sessions without a token stay signed in`() = runTest {
        val harness = harness(UnconfinedTestDispatcher(testScheduler))
        harness.users.upsert(localUser)
        harness.sessionStore.setUserId(LOCAL_USER_ID)

        val outcome = harness.repository.observeSession().first()

        assertEquals(Outcome.Success(localProfile), outcome)
        assertEquals(LOCAL_USER_ID, harness.sessionStore.value)
    }

    @Test
    fun `an expired stored token logs out on startup`() = runTest {
        val harness = harness(UnconfinedTestDispatcher(testScheduler))
        harness.users.upsert(localUser)
        harness.sessionStore.setUserId(LOCAL_USER_ID)
        harness.tokenStore.write(SessionToken("mock.expired", harness.clock.millis() - 1))

        val outcome = harness.repository.observeSession().first()

        assertEquals(Outcome.Success(null), outcome)
        assertNull(harness.sessionStore.value)
        assertNull(harness.tokenStore.read())
    }

    @Test
    fun `a 401 on an authenticated call signs the session out`() = runTest {
        val harness = harness(UnconfinedTestDispatcher(testScheduler))
        harness.repository.signIn(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD)
        harness.scenarios.select(MockScenario.UNAUTHORIZED)

        val outcome = harness.repository.updateProfile("Ava Updated")

        assertEquals(Outcome.Failure(AppError.Unauthorized), outcome)
        assertNull(harness.sessionStore.value)
        assertNull(harness.tokenStore.read())
    }

    @Test
    fun `updateProfile persists the server name`() = runTest {
        val harness = harness(UnconfinedTestDispatcher(testScheduler))
        val signedIn = (harness.repository.signIn(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD) as Outcome.Success).value

        val outcome = (harness.repository.updateProfile("Ava Updated") as Outcome.Success).value

        assertEquals("Ava Updated", outcome.displayName)
        assertEquals("Ava Updated", harness.users.rows[signedIn.id]?.displayName)
        assertEquals(signedIn.id, harness.sessionStore.value)
    }

    @Test
    fun `sign out clears tokens, the session and every cached row`() = runTest {
        val harness = harness(UnconfinedTestDispatcher(testScheduler))
        val signedIn = (harness.repository.signIn(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD) as Outcome.Success).value
        harness.transactions.upsert(transaction(signedIn.id))
        harness.budgets.upsert(budget(signedIn.id))
        harness.recurring.upsert(rule(signedIn.id))

        val outcome = harness.repository.signOut()

        assertTrue(outcome is Outcome.Success)
        assertNull(harness.sessionStore.value)
        assertNull(harness.tokenStore.read())
        assertTrue(harness.users.rows.isEmpty())
        assertTrue(harness.categories.rows.isEmpty())
        assertTrue(harness.accounts.rows.isEmpty())
        assertTrue(harness.transactions.rows.isEmpty())
        assertTrue(harness.budgets.rows.isEmpty())
        assertTrue(harness.recurring.rows.isEmpty())
    }

    private fun harness(
        dispatcher: CoroutineDispatcher,
        scenarios: MockScenarioController = MockScenarioController(),
    ): Harness = Harness(dispatcher, scenarios)

    private class Harness(
        dispatcher: CoroutineDispatcher,
        val scenarios: MockScenarioController,
    ) {
        val clock: Clock = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC)
        val users = FakeUserDao()
        val categories = FakeCategoryDao()
        val accounts = FakeAccountDao()
        val transactions = FakeTransactionDao()
        val budgets = FakeBudgetDao()
        val goals = FakeSavingsGoalDao()
        val recurring = FakeRecurringRuleDao()
        val outbox = FakeSyncOutboxDao()
        val sessionStore = FakeSessionStore()
        val tokenStore = FakeSessionTokenStore()

        private val dispatchers = object : CoroutineDispatchers {
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val main: CoroutineDispatcher = dispatcher
        }

        private val config = ApiConfig(
            environment = ApiEnvironment.STAGE,
            mode = ApiMode.MOCK,
            baseUrl = "https://mock.aram.local",
            appVersion = "test",
        )

        private val client = AramHttpClient.create(
            config = config,
            engine = MockApiEngine.create(
                store = MockApiStore(clock = clock),
                scenarios = scenarios,
                latencyMillis = { 0 },
            ),
            tokenProvider = SessionTokenProvider(tokenStore, sessionStore, clock),
        )

        val repository = AuthRepositoryImpl(
            client = client,
            endpoints = EndpointProvider(config),
            tokenStore = tokenStore,
            sessionStore = sessionStore,
            userDao = users,
            categoryDao = categories,
            accountDao = accounts,
            transactionDao = transactions,
            budgetDao = budgets,
            savingsGoalDao = goals,
            recurringRuleDao = recurring,
            outboxDao = outbox,
            syncMetadata = FakeSyncMetadataStore(),
            dispatchers = dispatchers,
            logger = Logger(),
            clock = clock,
        )
    }

    private companion object {
        const val LOCAL_USER_ID = "guest-1"
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L

        val localUser = UserEntity(
            id = LOCAL_USER_ID,
            displayName = "Guest",
            defaultCurrency = "USD",
            calendarSystem = CalendarSystem.GREGORIAN.name,
            createdAt = 1L,
            updatedAt = 1L,
        )

        val localProfile = UserProfile(
            id = LOCAL_USER_ID,
            displayName = "Guest",
            defaultCurrency = "USD",
            calendarSystem = CalendarSystem.GREGORIAN,
            createdAt = 1L,
        )

        fun transaction(userId: String) = TransactionEntity(
            id = "tx-1",
            userId = userId,
            type = "expense",
            accountId = "acc-1",
            categoryId = "cat-1",
            amountMinor = 1_000L,
            currencyCode = "IRR",
            occurredAt = 1L,
            note = null,
            transferGroupId = null,
            recurringRuleId = null,
            createdAt = 1L,
            updatedAt = 1L,
            deletedAt = null,
        )

        fun budget(userId: String) = BudgetEntity(
            id = "budget-1",
            userId = userId,
            categoryId = "cat-1",
            periodStartEpochDay = 20_000L,
            limitMinor = 10_000L,
            currencyCode = "IRR",
            createdAt = 1L,
            updatedAt = 1L,
            deletedAt = null,
        )

        fun rule(userId: String) = RecurringRuleEntity(
            id = "rule-1",
            userId = userId,
            accountId = "acc-1",
            categoryId = null,
            type = "expense",
            amountMinor = 500L,
            currencyCode = "IRR",
            frequency = "monthly",
            intervalCount = 1,
            startEpochDay = 20_000L,
            nextDueEpochDay = 20_030L,
            endEpochDay = null,
            isSubscription = false,
            note = null,
            active = true,
            createdAt = 1L,
            updatedAt = 1L,
            deletedAt = null,
        )
    }
}
