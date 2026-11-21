package com.aram.app.session

import com.aram.core.designsystem.state.UiState
import com.aram.core.domain.repository.AuthRepository
import com.aram.core.model.AppError
import com.aram.core.model.CalendarSystem
import com.aram.core.model.Outcome
import com.aram.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts loading then becomes empty when logged out`() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeAuthRepository(Outcome.Success(null)))

        assertEquals(UiState.Loading, viewModel.session.value)

        advanceUntilIdle()

        assertEquals(UiState.Empty, viewModel.session.value)
    }

    @Test
    fun `session with a profile becomes content`() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeAuthRepository(Outcome.Success(profile)))

        advanceUntilIdle()

        assertEquals(UiState.Content(profile), viewModel.session.value)
    }

    @Test
    fun `repository failure becomes an error state`() = runTest(dispatcher) {
        val error = AppError.Server(requestId = "req-1")
        val viewModel = MainViewModel(FakeAuthRepository(Outcome.Failure(error)))

        advanceUntilIdle()

        assertEquals(UiState.Error(error), viewModel.session.value)
    }

    @Test
    fun `retry re-subscribes and recovers from a failure`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(Outcome.Failure(AppError.Storage()))
        val viewModel = MainViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.session.value is UiState.Error)

        repository.session.value = Outcome.Success(profile)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(UiState.Content(profile), viewModel.session.value)
        assertEquals(2, repository.subscriptions)
    }

    private class FakeAuthRepository(
        initial: Outcome<UserProfile?>,
    ) : AuthRepository {

        val session = MutableStateFlow(initial)
        var subscriptions = 0

        override fun observeSession(): Flow<Outcome<UserProfile?>> {
            subscriptions++
            return session
        }

        override suspend fun createLocalProfile(displayName: String): Outcome<UserProfile> =
            Outcome.Failure(AppError.Unknown())

        override suspend fun register(
            displayName: String,
            email: String,
            password: String,
        ): Outcome<UserProfile> = Outcome.Failure(AppError.Unknown())

        override suspend fun signIn(email: String, password: String): Outcome<UserProfile> =
            Outcome.Failure(AppError.Unknown())

        override suspend fun updateProfile(displayName: String): Outcome<UserProfile> =
            Outcome.Failure(AppError.Unknown())

        override suspend fun signOut(): Outcome<Unit> = Outcome.Success(Unit)
    }

    private companion object {
        val profile = UserProfile(
            id = "user-1",
            displayName = "Aram",
            defaultCurrency = "IRR",
            calendarSystem = CalendarSystem.GREGORIAN,
            createdAt = 1L,
        )
    }
}
