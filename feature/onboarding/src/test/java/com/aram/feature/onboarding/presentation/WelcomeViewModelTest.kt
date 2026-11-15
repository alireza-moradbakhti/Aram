package com.aram.feature.onboarding.presentation

import com.aram.core.designsystem.state.SubmitState
import com.aram.core.domain.repository.AuthRepository
import com.aram.core.model.AppError
import com.aram.core.model.CalendarSystem
import com.aram.core.model.Outcome
import com.aram.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class WelcomeViewModelTest {

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
    fun `blank name shows the field error without calling the repository`() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = WelcomeViewModel(repository)

            viewModel.onNameChange("   ")
            viewModel.onCreateProfile()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showNameError)
            assertEquals(0, repository.createCalls)
            assertEquals(SubmitState.Idle, viewModel.uiState.value.submitState)
        }

    @Test
    fun `repository failure becomes a typed submit failure and keeps the name`() =
        runTest(dispatcher) {
            val error = AppError.Storage()
            val repository = FakeAuthRepository(result = Outcome.Failure(error))
            val viewModel = WelcomeViewModel(repository)

            viewModel.onNameChange("Aram")
            viewModel.onCreateProfile()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(SubmitState.Failed(error), state.submitState)
            assertEquals("Aram", state.name)
        }

    @Test
    fun `editing the name clears a previous failure`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(result = Outcome.Failure(AppError.Storage()))
        val viewModel = WelcomeViewModel(repository)

        viewModel.onNameChange("Aram")
        viewModel.onCreateProfile()
        advanceUntilIdle()

        viewModel.onNameChange("Aram B")

        assertEquals(SubmitState.Idle, viewModel.uiState.value.submitState)
    }

    @Test
    fun `success keeps the form submitting until the session switches`() =
        runTest(dispatcher) {
            val viewModel = WelcomeViewModel(FakeAuthRepository(result = Outcome.Success(profile)))

            viewModel.onNameChange("Aram")
            viewModel.onCreateProfile()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.submitState is SubmitState.Submitting)
        }

    @Test
    fun `double tap submits only once`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = WelcomeViewModel(repository)

        viewModel.onNameChange("Aram")
        viewModel.onCreateProfile()
        viewModel.onCreateProfile()
        advanceUntilIdle()

        assertEquals(1, repository.createCalls)
    }

    private class FakeAuthRepository(
        private val result: Outcome<UserProfile> = Outcome.Success(profile),
    ) : AuthRepository {

        var createCalls = 0

        override fun observeSession(): Flow<Outcome<UserProfile?>> =
            flowOf(Outcome.Success(null))

        override suspend fun createLocalProfile(displayName: String): Outcome<UserProfile> {
            createCalls++
            return result
        }
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
