package com.aram.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [androidx.room.testing.MigrationTestHelper] does not run under Robolectric
 * with Room 2.8, so the old file is rebuilt from the exported schema JSON and
 * then opened through the real database with the production migrations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AramDatabaseMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        context.getDatabasePath(DB_NAME).delete()
    }

    @Test
    fun `migrating 2 to 3 adds the slug column and the outbox table`() {
        createLegacyDatabase(version = 2) { db ->
            db.execSQL(
                "INSERT INTO categories (id, userId, name, kind, parentId, colorArgb, iconKey, " +
                    "sortOrder, archived, createdAt, updatedAt, deletedAt) VALUES " +
                    "('c1', '$USER', 'Groceries', 'EXPENSE', NULL, 0, 'cart', 0, 0, 0, 0, NULL)",
            )
            db.execSQL(
                "INSERT INTO transactions (id, userId, type, accountId, categoryId, amountMinor, " +
                    "currencyCode, occurredAt, note, transferGroupId, recurringRuleId, createdAt, " +
                    "updatedAt, deletedAt, pendingPush) VALUES ('t1', '$USER', 'EXPENSE', 'a1', " +
                    "'c1', 500, 'IRR', 0, 'lunch', NULL, NULL, 0, 0, NULL, 0)",
            )
        }

        val room = openMigrated()
        try {
            room.openHelper.readableDatabase.apply {
                query("SELECT name, slug FROM categories WHERE id = 'c1'").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("Groceries", cursor.getString(0))
                    assertNull(cursor.getString(1))
                }
                query("SELECT note, pendingPush FROM transactions WHERE id = 't1'").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("lunch", cursor.getString(0))
                    assertEquals(0, cursor.getInt(1))
                }
                query("SELECT COUNT(*) FROM sync_outbox").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(0, cursor.getInt(0))
                }
                query(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' " +
                        "AND tbl_name = 'sync_outbox' AND name LIKE 'index_%'",
                ).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(2, cursor.getInt(0))
                }
            }
        } finally {
            room.close()
        }
    }

    @Test
    fun `migrating from the oldest schema in one hop`() {
        createLegacyDatabase(version = 1) { db ->
            db.execSQL(
                "INSERT INTO transactions (id, userId, type, accountId, categoryId, amountMinor, " +
                    "currencyCode, occurredAt, note, transferGroupId, recurringRuleId, createdAt, " +
                    "updatedAt, deletedAt) VALUES ('t1', '$USER', 'EXPENSE', 'a1', NULL, 500, " +
                    "'IRR', 0, NULL, NULL, NULL, 0, 0, NULL)",
            )
        }

        val room = openMigrated()
        try {
            room.openHelper.readableDatabase.apply {
                query("SELECT note, pendingPush FROM transactions WHERE id = 't1'").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertNull(cursor.getString(0))
                    assertEquals(0, cursor.getInt(1))
                }
                query("SELECT COUNT(*) FROM sync_outbox").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(0, cursor.getInt(0))
                }
            }
        } finally {
            room.close()
        }
    }

    private fun openMigrated(): AramDatabase =
        Room.databaseBuilder(context, AramDatabase::class.java, DB_NAME)
            .addMigrations(*AramDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()

    private fun createLegacyDatabase(version: Int, seed: (SQLiteDatabase) -> Unit) {
        val file = context.getDatabasePath(DB_NAME).apply {
            parentFile?.mkdirs()
            delete()
        }
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = JSONObject(schemaText(version))
            .getJSONObject("database")
            .getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            if (entity.has("indices")) {
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) {
                    db.execSQL(
                        indices.getJSONObject(j).getString("createSql")
                            .replace("\${TABLE_NAME}", table),
                    )
                }
            }
        }
        seed(db)
        db.version = version
        db.close()
    }

    private fun schemaText(version: Int): String {
        val path = "${AramDatabase::class.java.canonicalName}/$version.json"
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: error("Missing exported schema at $path")
        return stream.bufferedReader().use { it.readText() }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
        const val USER = "user-1"
    }
}
