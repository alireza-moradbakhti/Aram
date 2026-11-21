package com.aram.core.network.mock

import com.aram.core.network.AramJson
import com.aram.core.network.dto.ChangePasswordRequest
import com.aram.core.network.dto.ContributeGoalRequest
import com.aram.core.network.dto.ConvertCurrencyRequest
import com.aram.core.network.dto.CreateGoalRequest
import com.aram.core.network.dto.DeleteAccountRequest
import com.aram.core.network.dto.ForgotPasswordRequest
import com.aram.core.network.dto.HealthCheckDto
import com.aram.core.network.dto.LoginRequest
import com.aram.core.network.dto.RegisterRequest
import com.aram.core.network.dto.ResetPasswordRequest
import com.aram.core.network.dto.ResultEnvelope
import com.aram.core.network.dto.SaveBudgetRequest
import com.aram.core.network.dto.SaveTransactionRequest
import com.aram.core.network.dto.SyncPushRequest
import com.aram.core.network.dto.UpdateBudgetRequest
import com.aram.core.network.dto.UpdateGoalRequest
import com.aram.core.network.dto.UpdateProfileRequest
import com.aram.core.network.dto.UpdateSettingsRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.io.IOException
import kotlin.random.Random

/**
 * Ktor engine that serves the Aram API from [MockApiStore]. Used by the alpha
 * build; tests build it with zero latency.
 *
 * Order per request: latency → scripted failure → routing.
 */
object MockApiEngine {

    const val DEFAULT_LATENCY_MIN_MS = 300L
    const val DEFAULT_LATENCY_MAX_MS = 800L
    const val SLOW_LATENCY_MS = 3_000L

    fun create(
        store: MockApiStore = MockApiStore(),
        scenarios: MockScenarioController = MockScenarioController(),
        latencyMillis: () -> Long = { Random.nextLong(DEFAULT_LATENCY_MIN_MS, DEFAULT_LATENCY_MAX_MS + 1) },
    ): MockEngine = MockEngine { request ->
        val latency = latencyMillis()
        if (latency > 0) delay(latency)

        when (scenarios.consumeOneShotOrNull()) {
            MockScenario.OFFLINE -> throw IOException("Mock API is offline.")
            MockScenario.SERVER_ERROR -> return@MockEngine failure(
                HttpStatusCode.InternalServerError,
                "Something went wrong on the server.",
            )
            MockScenario.UNAUTHORIZED -> return@MockEngine failure(
                HttpStatusCode.Unauthorized,
                "Your session has expired.",
            )
            MockScenario.SLOW -> delay(SLOW_LATENCY_MS)
            MockScenario.NORMAL, null -> Unit
        }

        route(request, store)
    }

    private fun MockScenarioController.consumeOneShotOrNull(): MockScenario? = when (val current = peek()) {
        MockScenario.NORMAL -> null
        else -> consumeOneShot().also { check(it == current) }
    }

    private suspend fun MockRequestHandleScope.route(request: HttpRequestData, store: MockApiStore): HttpResponseData {
        val path = request.url.encodedPath.removePrefix("/api").trimEnd('/')
        val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
        val method = request.method
        val token = request.headers[HttpHeaders.Authorization]
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() }
        val query = request.url.parameters
        val body = runCatching { request.body.toByteArray().decodeToString() }.getOrDefault("")
        val year = query["year"]?.toIntOrNull()
        val month = query["month"]?.toIntOrNull()

        return when {
            method == HttpMethod.Post && segments == listOf("auth", "register") ->
                withBody<RegisterRequest>(body) { answer(store.register(it)) }

            method == HttpMethod.Post && segments == listOf("auth", "login") ->
                withBody<LoginRequest>(body) { answer(store.login(it)) }

            method == HttpMethod.Post && segments == listOf("auth", "forgot-password") ->
                withBody<ForgotPasswordRequest>(body) { answer(store.forgotPassword(it)) }

            method == HttpMethod.Post && segments == listOf("auth", "reset-password") ->
                withBody<ResetPasswordRequest>(body) { answer(store.resetPassword(it)) }

            method == HttpMethod.Post && segments == listOf("auth", "change-password") ->
                withBody<ChangePasswordRequest>(body) { answer(store.changePassword(token, it)) }

            method == HttpMethod.Get && segments == listOf("auth", "me") -> answer(store.me(token))

            method == HttpMethod.Put && segments == listOf("auth", "profile") ->
                withBody<UpdateProfileRequest>(body) { answer(store.updateProfile(token, it)) }

            method == HttpMethod.Delete && segments == listOf("auth", "account") ->
                withBody<DeleteAccountRequest>(body) { answer(store.deleteAccount(token, it)) }

            method == HttpMethod.Get && segments == listOf("profile", "stats") -> answer(store.profileStats(token))

            method == HttpMethod.Get && segments == listOf("profile", "accounts") -> answer(store.accounts(token))

            method == HttpMethod.Get && segments == listOf("profile", "export") -> when (val outcome = store.exportCsv(token)) {
                is MockOutcome.Ok -> respond(
                    outcome.data,
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "text/csv; charset=utf-8"),
                )
                is MockOutcome.Error -> answer(outcome)
            }

            method == HttpMethod.Get && segments == listOf("categories") ->
                answer(store.categories(query["kind"]))

            method == HttpMethod.Get && segments == listOf("transactions") -> answer(store.listTransactions(
                token,
                year = year,
                month = month,
                kind = query["kind"],
                page = query["page"]?.toIntOrNull() ?: 1,
                pageSize = query["pageSize"]?.toIntOrNull() ?: 50,
                search = query["search"],
            ))

            method == HttpMethod.Get && segments == listOf("transactions", "summary") ->
                answer(store.transactionSummary(token, year, month))

            method == HttpMethod.Get && segments == listOf("transactions", "insights", "weekly") ->
                answer(store.weeklyInsight(token))

            method == HttpMethod.Post && segments == listOf("transactions") ->
                withBody<SaveTransactionRequest>(body) { answer(store.createTransaction(token, it)) }

            method == HttpMethod.Put && segments.size == 2 && segments[0] == "transactions" ->
                withBody<SaveTransactionRequest>(body) { answer(store.updateTransaction(token, segments[1], it)) }

            method == HttpMethod.Delete && segments.size == 2 && segments[0] == "transactions" ->
                answer(store.deleteTransaction(token, segments[1]))

            method == HttpMethod.Get && segments == listOf("budgets", "overview") ->
                answer(store.budgetOverview(token, year, month))

            method == HttpMethod.Get && segments == listOf("budgets", "categories") ->
                answer(store.budgetCategories())

            method == HttpMethod.Get && segments == listOf("budgets") -> answer(store.budgets(token))

            method == HttpMethod.Post && segments == listOf("budgets") ->
                withBody<SaveBudgetRequest>(body) { answer(store.createBudget(token, it)) }

            method == HttpMethod.Put && segments.size == 2 && segments[0] == "budgets" ->
                withBody<UpdateBudgetRequest>(body) { answer(store.updateBudget(token, segments[1], it)) }

            method == HttpMethod.Delete && segments.size == 2 && segments[0] == "budgets" ->
                answer(store.deleteBudget(token, segments[1]))

            method == HttpMethod.Get && segments == listOf("goals") ->
                answer(store.goals(token, includeArchived = query["includeArchived"] == "true"))

            method == HttpMethod.Post && segments == listOf("goals") ->
                withBody<CreateGoalRequest>(body) { answer(store.createGoal(token, it)) }

            method == HttpMethod.Put && segments.size == 2 && segments[0] == "goals" ->
                withBody<UpdateGoalRequest>(body) { answer(store.updateGoal(token, segments[1], it)) }

            method == HttpMethod.Delete && segments.size == 2 && segments[0] == "goals" ->
                answer(store.deleteGoal(token, segments[1]))

            method == HttpMethod.Post && segments.size == 3 && segments[0] == "goals" &&
                segments[2] == "contributions" ->
                withBody<ContributeGoalRequest>(body) { answer(store.contribute(token, segments[1], it)) }

            method == HttpMethod.Get && segments == listOf("notifications") ->
                answer(store.notifications(token, query["scope"]))

            method == HttpMethod.Post && segments == listOf("notifications", "read-all") ->
                answer(store.markAllNotificationsRead(token))

            method == HttpMethod.Post && segments.size == 3 && segments[0] == "notifications" &&
                segments[2] == "read" ->
                answer(store.markNotification(token, segments[1], read = true))

            method == HttpMethod.Post && segments.size == 3 && segments[0] == "notifications" &&
                segments[2] == "unread" ->
                answer(store.markNotification(token, segments[1], read = false))

            method == HttpMethod.Get && segments == listOf("dashboard") -> answer(store.dashboard(token, year, month))

            method == HttpMethod.Get && segments == listOf("settings") -> answer(store.settings(token))

            method == HttpMethod.Put && segments == listOf("settings") ->
                withBody<UpdateSettingsRequest>(body) { answer(store.updateSettings(token, it)) }

            method == HttpMethod.Get && segments == listOf("settings", "currencies") -> answer(store.currencies())

            method == HttpMethod.Get && segments == listOf("settings", "locales") -> answer(store.locales())

            method == HttpMethod.Get && segments == listOf("rates") -> answer(store.currencies())

            method == HttpMethod.Post && segments == listOf("settings", "convert") ->
                withBody<ConvertCurrencyRequest>(body) { answer(store.convert(it)) }

            method == HttpMethod.Post && segments == listOf("sync", "push") ->
                withBody<SyncPushRequest>(body) { answer(store.syncPush(token, it)) }

            method == HttpMethod.Get && segments == listOf("sync", "pull") ->
                answer(store.syncPull(token, query["since"]))

            method == HttpMethod.Get && segments == listOf("health") -> respondJson(
                HttpStatusCode.OK,
                AramJson.encodeToString(HealthCheckDto(status = "Healthy", totalDuration = "00:00:00.0040000")),
            )

            else -> failure(HttpStatusCode.NotFound, "No mock route for ${method.value} $path.")
        }
    }

    private inline fun <reified T> MockRequestHandleScope.withBody(
        raw: String,
        block: (T) -> HttpResponseData,
    ): HttpResponseData {
        val parsed = runCatching { AramJson.decodeFromString<T>(raw) }.getOrNull()
            ?: return failure(HttpStatusCode.BadRequest, "The request body is invalid.")
        return block(parsed)
    }

    private inline fun <reified T> MockRequestHandleScope.answer(outcome: MockOutcome<T>): HttpResponseData =
        when (outcome) {
            is MockOutcome.Ok -> respondJson(
                HttpStatusCode.fromValue(outcome.status),
                AramJson.encodeToString(ResultEnvelope(isSuccess = true, data = outcome.data)),
            )
            is MockOutcome.Error -> answer(outcome)
        }

    private fun MockRequestHandleScope.answer(error: MockOutcome.Error): HttpResponseData {
        val code = HttpStatusCode.fromValue(error.status)
        return if (error.fields.isEmpty()) {
            failure(code, error.message)
        } else {
            respondJson(
                code,
                AramJson.encodeToString(
                    FieldFailureBody(statusCode = error.status, message = error.message, errors = error.fields),
                ),
            )
        }
    }

    private fun MockRequestHandleScope.respondJson(status: HttpStatusCode, body: String): HttpResponseData = respond(
        body,
        status,
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    private fun MockRequestHandleScope.failure(status: HttpStatusCode, message: String): HttpResponseData = respondJson(
        status,
        AramJson.encodeToString(FailureBody(message = message, errors = listOf(message))),
    )

    @Serializable
    private data class FailureBody(
        val isSuccess: Boolean = false,
        val message: String,
        val errors: List<String> = emptyList(),
        val data: String? = null,
    )

    @Serializable
    private data class FieldFailureBody(
        val statusCode: Int,
        val message: String,
        val errors: Map<String, List<String>>,
    )
}
