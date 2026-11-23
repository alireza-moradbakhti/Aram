package com.aram.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.syncPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "sync_metadata",
)

@Singleton
class SyncMetadataDataStore @Inject constructor(
    @ApplicationContext context: Context,
) : SyncMetadataStore {

    private val dataStore = context.syncPreferences

    override suspend fun read(): SyncMetadata? {
        val preferences = dataStore.data.first()
        val lastSyncedAt = preferences[Keys.LastSyncedAt] ?: return null
        val month = preferences[Keys.Month] ?: return null
        return SyncMetadata(
            lastSyncedAtMillis = lastSyncedAt,
            month = month,
            cursor = preferences[Keys.Cursor],
        )
    }

    override suspend fun write(metadata: SyncMetadata) {
        dataStore.edit { preferences ->
            preferences[Keys.LastSyncedAt] = metadata.lastSyncedAtMillis
            preferences[Keys.Month] = metadata.month
            val cursor = metadata.cursor
            if (cursor == null) {
                preferences.remove(Keys.Cursor)
            } else {
                preferences[Keys.Cursor] = cursor
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private object Keys {
        val LastSyncedAt = longPreferencesKey("last_synced_at")
        val Month = stringPreferencesKey("last_synced_month")
        val Cursor = stringPreferencesKey("sync_cursor")
    }
}
