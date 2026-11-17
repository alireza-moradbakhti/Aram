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
import org.junit.Assert.assertFalse
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
    fun `empty form flags every field without calling the repository`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = WelcomeViewModel(repository)

        viewModel.onCreateAccount()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showNameError)
        assertTrue(state.showEmailError)
        assertTrue(state.showPasswordError)
        assertTrue(state.showTermsError)
        assertEquals(0, repository.createCalls)
        assertEquals(SubmitState.Idle, state.submitState)
    }

    @Test
    fun `invalid email and weak password block submission`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = WelcomeViewModel(repository)

        viewModel.onNameChange("Aram")
        viewModel.onEmailChange("aram@")
        viewModel.onPasswordChange("123")
        viewModel.onAcceptedTermsChange(true)
        viewModel.onCreateAccount()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showEmailError)
        assertTrue(viewModel.uiState.value.showPasswordError)
        assertFalse(viewModel.uiState.value.showNameError)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `valid form creates the profile with the trimmed name`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = WelcomeViewModel(repository)
        fillValidForm(viewModel)

        viewModel.onCreateAccount()
        advanceUntilIdle()

        assertEquals(1, repository.createCalls)
        assertEquals("Aram", repository.lastName)
    }

    @Test
    fun `repository failure becomes a typed submit failure and keeps the form`() =
        runTest(dispatcher) {
            val error = AppError.Storage()
            val repository = FakeAuthRepository(result = Outcome.Failure(error))
            val viewModel = WelcomeViewModel(repository)
            fillValidForm(viewModel)

            viewModel.onCreateAccount()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(SubmitState.Failed(error), state.submitState)
            assertEquals("Aram", state.name)
            assertEquals("aram@example.com", state.email)
        }

    @Test
    fun `editing a field clears a previous failure`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(result = Outcome.Failure(AppError.Storage()))
        val viewModel = WelcomeViewModel(repository)
        fillValidForm(viewModel)
        viewModel.onCreateAccount()
        advanceUntilIdle()

        viewModel.onNameChange("Aram B")

        assertEquals(SubmitState.Idle, viewModel.uiState.value.submitState)
    }

    @Test
    fun `success keeps the form submitting until the session switches`() = runTest(dispatcher) {
        val viewModel = WelcomeViewModel(FakeAuthRepository(result = Outcome.Success(profile)))
        fillValidForm(viewModel)

        viewModel.onCreateAccount()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.submitState is SubmitState.Submitting)
    }

    @Test
    fun `double tap submits only once`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = WelcomeViewModel(repository)
        fillValidForm(viewModel)

        viewModel.onCreateAccount()
        viewModel.onCreateAccount()
        advanceUntilIdle()

        assertEquals(1, repository.createCalls)
    }

    @Test
    fun `password strength follows the prototype scoring`() {
        assertEquals(0, passwordStrengthScore(""))
        assertEquals(1, passwordStrengthScore("abc"))
        assertEquals(2, passwordStrengthScore("abcdef"))
        assertEquals(3, passwordStrengthScore("aram1234"))
        assertEquals(2, passwordStrengthScore("abcdefgh"))
    }

    @Test
    fun `email validation accepts plain addresses only`() {
        assertTrue(isValidEmail("aram@example.com"))
        assertTrue(isValidEmail(" aram.morad+bills@mail.example.ir "))
        assertFalse(isValidEmail("aram@"))
        assertFalse(isValidEmail("aram.example.com"))
        assertFalse(isValidEmail(""))
    }

    private fun fillValidForm(viewModel: WelcomeViewModel) {
        viewModel.onNameChange("Aram")
        viewModel.onEmailChange("aram@example.com")
        viewModel.onPasswordChange("aram1234")
        viewModel.onAcceptedTermsChange(true)
    }

    private class FakeAuthRepository(
        private val result: Outcome<UserProfile> = Outcome.Success(profile),
    ) : AuthRepository {

        var createCalls = 0
        var lastName: String? = null

        override fun observeSession(): Flow<Outcome<UserProfile?>> =
            flowOf(Outcome.Success(null))

        override suspend fun createLocalProfile(displayName: String): Outcome<UserProfile> {
            createCalls++
            lastName = displayName
            return result
        }

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
