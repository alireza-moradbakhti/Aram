package com.aram.core.network.mock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Scripted failures the alpha mock can produce on demand. */
enum class MockScenario(val label: String) {
    NORMAL("Normal"),
    OFFLINE("Offline"),
    SERVER_ERROR("Server error"),
    UNAUTHORIZED("Unauthorized"),
    SLOW("Slow network"),
    ;

    val isFailure: Boolean get() = this != NORMAL
}

/**
 * Runtime switch for [MockScenario]. Only the in-memory mock reads it; remote
 * builds can hold one but it changes nothing.
 *
 * [UNAUTHORIZED] fires once (then returns to normal) so the app can test the
 * sign-out path without being stuck; the others persist until changed.
 */
class MockScenarioController(initial: MockScenario = MockScenario.NORMAL) {

    private val state = MutableStateFlow(initial)

    val scenario: StateFlow<MockScenario> = state.asStateFlow()

    fun select(value: MockScenario) {
        state.value = value
    }

    fun reset() {
        state.value = MockScenario.NORMAL
    }

    internal fun peek(): MockScenario = state.value

    internal fun consumeOneShot(): MockScenario {
        val current = state.value
        if (current == MockScenario.UNAUTHORIZED) state.value = MockScenario.NORMAL
        return current
    }
}
