package com.aram.core.data.repository

import android.database.sqlite.SQLiteException
import com.aram.core.common.CoroutineDispatchers
import com.aram.core.common.error.safeOutcome
import com.aram.core.common.error.toAppError
import com.aram.core.data.defaults.DefaultData
import com.aram.core.data.mapper.toModel
import com.aram.core.database.dao.AccountDao
import com.aram.core.database.dao.BudgetDao
import com.aram.core.database.dao.CategoryDao
import com.aram.core.database.dao.RecurringRuleDao
import com.aram.core.database.dao.SavingsGoalDao
import com.aram.core.database.dao.TransactionDao
import com.aram.core.database.dao.UserDao
import com.aram.core.database.entity.UserEntity
import com.aram.core.datastore.SessionStore
import com.aram.core.datastore.SessionToken
import com.aram.core.datastore.SessionTokenStore
import com.aram.core.datastore.SyncMetadataStore
import com.aram.core.domain.repository.AuthRepository
import com.aram.core.logging.Logger
import com.aram.core.model.AppError
import com.aram.core.model.CalendarSystem
import com.aram.core.model.Outcome
import com.aram.core.model.UserProfile
import com.aram.core.network.api.ApiResult
import com.aram.core.network.api.safeApiCall
import com.aram.core.network.api.toAppError as apiToAppError
import com.aram.core.network.config.ApiPaths
import com.aram.core.network.config.EndpointProvider
import com.aram.core.network.dto.AuthResponseDto
import com.aram.core.network.dto.LoginRequest
import com.aram.core.network.dto.ProfileDto
import com.aram.core.network.dto.RegisterRequest
import com.aram.core.network.dto.UpdateProfileRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val client: HttpClient,
    private val endpoints: EndpointProvider,
    private val tokenStore: SessionTokenStore,
    private val sessionStore: SessionStore,
    private val userDao: UserDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringRuleDao: RecurringRuleDao,
    private val syncMetadata: SyncMetadataStore,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger,
    private val clock: Clock,
) : AuthRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSession(): Flow<Outcome<UserProfile?>> =
        flow {
            if (dropExpiredToken()) {
                emit(null)
            }
            emitAll(
                sessionStore.userId.flatMapLatest { userId ->
                    if (userId == null) {
                        flowOf(null)
                    } else {
                        userDao.observeById(userId).map { it?.toModel() }
                    }
                },
            )
        }
            .map<UserProfile?, Outcome<UserProfile?>> { Outcome.Success(it) }
            .catch { e ->
                if (e is CancellationException) throw e
                logger.e(COMPONENT, "observeSession failed", e)
                emit(Outcome.Failure(e.toStorageAwareError()))
            }
            .flowOn(dispatchers.io)

    override suspend fun createLocalProfile(displayName: String): Outcome<UserProfile> {
        val name = displayName.trim()
        if (name.isEmpty()) {
            return Outcome.Failure(
                AppError.Validation(mapOf("displayName" to "Display name is required")),
            )
        }

        return safeOutcome(
            component = COMPONENT,
            operation = "createLocalProfile",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                val now = clock.millis()
                val userId = UUID.randomUUID().toString()
                val user = UserEntity(
                    id = userId,
                    displayName = name,
                    defaultCurrency = DefaultData.DEFAULT_CURRENCY,
                    calendarSystem = CalendarSystem.GREGORIAN.name,
                    createdAt = now,
                    updatedAt = now,
                )

                userDao.upsert(user)
                categoryDao.upsertAll(DefaultData.categories(userId, now))
                accountDao.upsertAll(listOf(DefaultData.cashAccount(userId, now)))
                sessionStore.setUserId(userId)

                user.toModel()
            }
        }
    }

    override suspend fun register(
        displayName: String,
        email: String,
        password: String,
    ): Outcome<UserProfile> {
        val name = displayName.trim()
        val address = email.trim()
        val fields = buildMap {
            if (name.isEmpty()) put("displayName", DISPLAY_NAME_REQUIRED)
            if (!isEmail(address)) put("email", "Enter a valid email address")
            if (!isValidPassword(password)) put("password", PASSWORD_RULE)
        }
        if (fields.isNotEmpty()) return Outcome.Failure(AppError.Validation(fields))

        return authenticate {
            safeApiCall(
                client = client,
                logger = logger,
                method = HttpMethod.Post,
                url = endpoints.url(ApiPaths.AUTH_REGISTER),
                configure = {
                    contentType(ContentType.Application.Json)
                    setBody(RegisterRequest(email = address, password = password, displayName = name))
                },
            )
        }
    }

    override suspend fun signIn(email: String, password: String): Outcome<UserProfile> {
        val address = email.trim()
        val fields = buildMap {
            if (!isEmail(address)) put("email", "Enter a valid email address")
            if (password.isEmpty()) put("password", "Enter your password")
        }
        if (fields.isNotEmpty()) return Outcome.Failure(AppError.Validation(fields))

        return authenticate {
            safeApiCall(
                client = client,
                logger = logger,
                method = HttpMethod.Post,
                url = endpoints.url(ApiPaths.AUTH_LOGIN),
                configure = {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email = address, password = password))
                },
            )
        }
    }

    override suspend fun updateProfile(displayName: String): Outcome<UserProfile> {
        val name = displayName.trim()
        if (name.isEmpty()) {
            return Outcome.Failure(AppError.Validation(mapOf("displayName" to DISPLAY_NAME_REQUIRED)))
        }

        val userId = sessionStore.userId.first()
            ?: return Outcome.Failure(AppError.Unauthorized)
        if (tokenStore.read() == null) return renameLocalProfile(userId, name)

        return when (
            val result = safeApiCall<ProfileDto>(
                client = client,
                logger = logger,
                method = HttpMethod.Put,
                url = endpoints.url(ApiPaths.PROFILE),
                configure = {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateProfileRequest(displayName = name))
                },
            )
        ) {
            is ApiResult.Failure -> Outcome.Failure(result.error.apiToAppError())
            is ApiResult.Success -> safeOutcome(
                component = COMPONENT,
                operation = "updateProfile",
                logger = logger,
                mapError = { it.toStorageAwareError() },
            ) {
                withContext(dispatchers.io) { persistProfile(result.data) }
            }
        }
    }

    override suspend fun signOut(): Outcome<Unit> =
        safeOutcome(
            component = COMPONENT,
            operation = "signOut",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                val userId = sessionStore.userId.first()
                sessionStore.clear()
                tokenStore.clear()
                if (userId != null) wipeUser(userId)
            }
        }

    /**
     * The API has no refresh endpoint: an expired stored token ends the session
     * on next start. Guests (no token) are unaffected.
     */
    private suspend fun dropExpiredToken(): Boolean {
        val token = tokenStore.read() ?: return false
        if (!token.isExpired(clock.millis())) return false
        clearCredentials()
        logger.i(COMPONENT, "Stored session token expired; signed out")
        return true
    }

    private suspend fun clearCredentials() {
        tokenStore.clear()
        sessionStore.clear()
    }

    private suspend fun authenticate(
        call: suspend () -> ApiResult<AuthResponseDto>,
    ): Outcome<UserProfile> = when (val result = call()) {
        is ApiResult.Failure -> Outcome.Failure(result.error.apiToAppError())
        is ApiResult.Success -> safeOutcome(
            component = COMPONENT,
            operation = "authenticate",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) { startSession(result.data) }
        }
    }

    private suspend fun startSession(response: AuthResponseDto): UserProfile {
        val now = clock.millis()
        tokenStore.write(
            SessionToken(
                value = response.token,
                expiresAtEpochMillis = parseExpiry(response.expiresAtUtc, now),
            ),
        )
        val profile = persistProfile(response.profile)
        sessionStore.setUserId(profile.id)
        return profile
    }

    private suspend fun persistProfile(dto: ProfileDto): UserProfile {
        val now = clock.millis()
        val existing = userDao.getById(dto.id)
        val entity = UserEntity(
            id = dto.id,
            displayName = dto.displayName.ifBlank { existing?.displayName ?: FALLBACK_NAME },
            defaultCurrency = dto.homeCurrency.ifBlank {
                existing?.defaultCurrency ?: DefaultData.DEFAULT_CURRENCY
            },
            calendarSystem = existing?.calendarSystem ?: CalendarSystem.GREGORIAN.name,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        userDao.upsert(entity)
        if (categoryDao.countByUser(entity.id) == 0) {
            categoryDao.upsertAll(DefaultData.categories(entity.id, now))
        }
        if (accountDao.countByUser(entity.id) == 0) {
            accountDao.upsertAll(listOf(DefaultData.cashAccount(entity.id, now)))
        }
        return entity.toModel()
    }

    private suspend fun renameLocalProfile(userId: String, name: String): Outcome<UserProfile> {
        val existing = userDao.getById(userId)
            ?: return Outcome.Failure(AppError.NotFound)
        val updated = existing.copy(displayName = name, updatedAt = clock.millis())
        return safeOutcome(
            component = COMPONENT,
            operation = "renameLocalProfile",
            logger = logger,
            mapError = { it.toStorageAwareError() },
        ) {
            withContext(dispatchers.io) {
                userDao.upsert(updated)
                updated.toModel()
            }
        }
    }

    private suspend fun wipeUser(userId: String) {
        transactionDao.deleteByUser(userId)
        budgetDao.deleteByUser(userId)
        savingsGoalDao.deleteContributionsByUser(userId)
        savingsGoalDao.deleteGoalsByUser(userId)
        recurringRuleDao.deleteByUser(userId)
        accountDao.deleteByUser(userId)
        categoryDao.deleteByUser(userId)
        userDao.deleteById(userId)
        syncMetadata.clear()
    }

    private fun parseExpiry(expiresAtUtc: String, now: Long): Long =
        runCatching { Instant.parse(expiresAtUtc).toEpochMilli() }
            .getOrElse { now + FALLBACK_SESSION_MILLIS }

    private fun isEmail(value: String): Boolean =
        value.length >= 5 && value.substringBefore('@').isNotBlank() &&
            value.substringAfter('@', "").contains('.')

    private fun isValidPassword(value: String): Boolean =
        value.length >= 8 && value.any { it.isLowerCase() } && value.any { it.isDigit() }

    private fun Throwable.toStorageAwareError(): AppError = when (this) {
        is SQLiteException -> AppError.Storage(this)
        else -> toAppError()
    }

    private companion object {
        const val COMPONENT = "AuthRepository"
        const val DISPLAY_NAME_REQUIRED = "Display name is required"
        const val PASSWORD_RULE = "Use at least 8 characters with a lowercase letter and a digit."
        const val FALLBACK_NAME = "Aram"
        const val FALLBACK_SESSION_MILLIS = 24L * 60L * 60L * 1000L
    }
}
