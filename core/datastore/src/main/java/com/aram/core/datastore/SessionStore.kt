package com.aram.core.datastore

import kotlinx.coroutines.flow.Flow

/** The session seam repositories depend on; [SessionDataStore] is the app implementation. */
interface SessionStore {

    val userId: Flow<String?>

    suspend fun setUserId(userId: String)

    suspend fun clear()
}
