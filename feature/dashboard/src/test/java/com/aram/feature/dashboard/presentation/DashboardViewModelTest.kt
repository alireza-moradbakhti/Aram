package com.aram.feature.dashboard.presentation

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

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
    fun `uiState streams loading then content`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel()

        viewModel.uiState.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()
            assertFalse(awaitItem().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `starts in the loading state`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `keeps loading until the placeholder delay elapses`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel()

        advanceTimeBy(300)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `settles to content once the load completes`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }
}
