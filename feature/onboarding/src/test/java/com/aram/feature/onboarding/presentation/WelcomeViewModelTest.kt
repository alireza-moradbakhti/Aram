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

    @Test
    fun `switching to sign in clears the password and errors`() = runTest(dispatcher) {
        val viewModel = WelcomeViewModel(FakeAuthRepository(result = Outcome.Failure(AppError.Storage())))

        viewModel.onNameChange("Aram")
        viewModel.onCreateProfile()
        advanceUntilIdle()

        viewModel.onOpenSignIn()

        val state = viewModel.uiState.value
        assertEquals(AuthScreen.SignIn, state.screen)
        assertEquals(SubmitState.Idle, state.submitState)
        assertTrue(state.fieldErrors.isEmpty())
    }

    @Test
    fun `sign in sends trimmed credentials`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = WelcomeViewModel(repository)

        viewModel.onOpenSignIn()
        viewModel.onEmailChange("  demo@aram.app ")
        viewModel.onPasswordChange("demo1234")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals("demo@aram.app", repository.lastEmail)
        assertEquals("demo1234", repository.lastPassword)
        assertEquals(1, repository.signInCalls)
    }

    @Test
    fun `validation failure lands in field errors without a generic alert`() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(
                result = Outcome.Failure(
                    AppError.Validation(mapOf("email" to "Enter a valid email address")),
                ),
            )
            val viewModel = WelcomeViewModel(repository)

            viewModel.onOpenSignUp()
            viewModel.onNameChange("Aram")
            viewModel.onEmailChange("not-an-email")
            viewModel.onPasswordChange("demo1234")
            viewModel.onSubmit()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Enter a valid email address", state.fieldErrors["email"])
            assertEquals(SubmitState.Idle, state.submitState)
        }

    @Test
    fun `sign up requires the terms checkbox`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = WelcomeViewModel(repository)

        viewModel.onOpenSignUp()
        viewModel.onTermsChange(false)
        viewModel.onNameChange("Aram")
        viewModel.onEmailChange("aram@example.com")
        viewModel.onPasswordChange("aram1234")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals("Accept the Terms to continue", viewModel.uiState.value.fieldErrors["terms"])
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `sign up sends name email and password`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = WelcomeViewModel(repository)

        viewModel.onOpenSignUp()
        viewModel.onNameChange("Aram Moradi")
        viewModel.onEmailChange("aram@example.com")
        viewModel.onPasswordChange("aram1234")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals("Aram Moradi", repository.lastName)
        assertEquals("aram@example.com", repository.lastEmail)
        assertEquals("aram1234", repository.lastPassword)
        assertEquals(1, repository.registerCalls)
    }

    @Test
    fun `demo shortcut fills the sign in form`() = runTest(dispatcher) {
        val viewModel = WelcomeViewModel(FakeAuthRepository())
        val demo = DemoCredentials(email = "demo@aram.app", password = "demo1234")

        viewModel.onUseDemo(demo)

        val state = viewModel.uiState.value
        assertEquals(AuthScreen.SignIn, state.screen)
        assertEquals("demo@aram.app", state.email)
        assertEquals("demo1234", state.password)
        assertFalse(state.passwordVisible)
    }

    @Test
    fun `password strength follows the meter rules`() {
        assertEquals(PasswordStrength.Empty, strengthOf(""))
        assertEquals(PasswordStrength.Weak, strengthOf("abcdefgh"))
        assertEquals(PasswordStrength.Fair, strengthOf("aram1234"))
        assertEquals(PasswordStrength.Strong, strengthOf("Aram1234Secure!"))
    }

    private class FakeAuthRepository(
        private val result: Outcome<UserProfile> = Outcome.Success(profile),
    ) : AuthRepository {

        var createCalls = 0
        var registerCalls = 0
        var signInCalls = 0
        var lastName: String? = null
        var lastEmail: String? = null
        var lastPassword: String? = null

        override fun observeSession(): Flow<Outcome<UserProfile?>> =
            flowOf(Outcome.Success(null))

        override suspend fun createLocalProfile(displayName: String): Outcome<UserProfile> {
            createCalls++
            return result
        }

        override suspend fun register(
            displayName: String,
            email: String,
            password: String,
        ): Outcome<UserProfile> {
            registerCalls++
            lastName = displayName.trim()
            lastEmail = email.trim()
            lastPassword = password
            return result
        }

        override suspend fun signIn(email: String, password: String): Outcome<UserProfile> {
            signInCalls++
            lastEmail = email.trim()
            lastPassword = password
            return result
        }

        override suspend fun updateProfile(displayName: String): Outcome<UserProfile> = result

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
