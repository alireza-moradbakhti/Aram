package com.aram.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "session",
)

@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.sessionPreferences

    val userId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[Keys.UserId]
    }

    suspend fun setUserId(userId: String) {
        dataStore.edit { preferences ->
            preferences[Keys.UserId] = userId
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private object Keys {
        val UserId = stringPreferencesKey("user_id")
    }
}
