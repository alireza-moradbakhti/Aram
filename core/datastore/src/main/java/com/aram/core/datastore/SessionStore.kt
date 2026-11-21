package com.aram.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * The signed-in user id. Guests created offline have an id but no token; a
 * session id survives process death until [clear].
 */
interface SessionStore {

    val userId: Flow<String?>

    suspend fun setUserId(userId: String)

    suspend fun clear()
}
