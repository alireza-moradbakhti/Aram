package com.aram.core.data.repository

import com.aram.core.common.CoroutineDispatchers
import com.aram.core.data.defaults.DefaultData
import com.aram.core.data.mapper.toModel
import com.aram.core.database.dao.AccountDao
import com.aram.core.database.dao.CategoryDao
import com.aram.core.database.dao.UserDao
import com.aram.core.database.entity.UserEntity
import com.aram.core.datastore.SessionDataStore
import com.aram.core.domain.repository.AuthRepository
import com.aram.core.model.CalendarSystem
import com.aram.core.model.UserProfile
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val sessionDataStore: SessionDataStore,
    private val dispatchers: CoroutineDispatchers,
) : AuthRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSession(): Flow<UserProfile?> =
        sessionDataStore.userId
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(null)
                } else {
                    userDao.observeById(userId).map { entity -> entity?.toModel() }
                }
            }
            .flowOn(dispatchers.io)

    override suspend fun createLocalProfile(displayName: String): UserProfile =
        withContext(dispatchers.io) {
            val name = displayName.trim()
            require(name.isNotEmpty()) { "Display name cannot be blank" }

            val now = System.currentTimeMillis()
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
            sessionDataStore.setUserId(userId)

            user.toModel()
        }
}
