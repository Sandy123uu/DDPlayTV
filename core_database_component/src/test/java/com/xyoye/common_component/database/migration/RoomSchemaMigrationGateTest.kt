package com.xyoye.common_component.database.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xyoye.common_component.database.DatabaseInfo
import com.xyoye.common_component.database.DatabaseManager
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomSchemaMigrationGateTest {
    @Test
    fun schemaExportEnabledAndMigration15To16Works() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        deleteDatabaseFiles(context, TEST_DB_NAME)

        val dbFile =
            context.getDatabasePath(TEST_DB_NAME).also { file ->
                file.parentFile?.mkdirs()
            }

        val schemaJson = loadSchemaJson(version = OLD_VERSION)
        loadSchemaJson(version = NEW_VERSION)
        createDatabaseFromRoomSchemaJson(dbFile, schemaJson, version = OLD_VERSION)

        val roomDb =
            Room
                .databaseBuilder(context, DatabaseInfo::class.java, TEST_DB_NAME)
                .addMigrations(DatabaseManager.MIGRATION_15_16)
                .build()

        try {
            val supportDb = roomDb.openHelper.writableDatabase

            val cursor = supportDb.query("PRAGMA table_info(`media_library`)")
            val columnNames = mutableSetOf<String>()
            cursor.use {
                val nameIndex = it.getColumnIndex("name")
                while (it.moveToNext()) {
                    if (nameIndex >= 0) {
                        columnNames.add(it.getString(nameIndex))
                    }
                }
            }

            assertTrue(
                "Migration 15->16 must add column web_dav_allow_insecure_tls to media_library",
                columnNames.contains("web_dav_allow_insecure_tls"),
            )
        } finally {
            roomDb.close()
            deleteDatabaseFiles(context, TEST_DB_NAME)
        }
    }

    private fun loadSchemaJson(version: Int): JSONObject {
        val workingDir = checkNotNull(System.getProperty("user.dir")) { "System property user.dir is null" }
        val root =
            findProjectRoot(File(workingDir))
                ?: error("Cannot locate project root from user.dir=$workingDir")
        val canonicalName =
            checkNotNull(DatabaseInfo::class.java.canonicalName) { "DatabaseInfo canonicalName is null" }
        val schemaFile = File(root, "core_database_component/schemas/$canonicalName/$version.json")
        require(schemaFile.exists()) {
            "Missing Room schema file: ${schemaFile.absolutePath}. " +
                "Run :core_database_component:kaptDebugKotlin (or :core_database_component:testDebugUnitTest) " +
                "and commit generated schemas."
        }
        return JSONObject(schemaFile.readText())
    }

    private fun createDatabaseFromRoomSchemaJson(
        dbFile: File,
        schemaJson: JSONObject,
        version: Int
    ) {
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { database ->
            database.beginTransaction()
            try {
                val db = schemaJson.getJSONObject("database")

                val entities = db.getJSONArray("entities")
                forEachObject(entities) { entity ->
                    val tableName = entity.getString("tableName")
                    val tableSql =
                        entity
                            .getString("createSql")
                            .replace(ROOM_TABLE_NAME_PLACEHOLDER, tableName)
                    database.execSQL(tableSql)

                    val indices = entity.optJSONArray("indices")
                    if (indices != null) {
                        forEachObject(indices) { index ->
                            val createIndexSql =
                                index
                                    .optString("createSql")
                                    .replace(ROOM_TABLE_NAME_PLACEHOLDER, tableName)
                            if (createIndexSql.isNullOrBlank().not()) {
                                database.execSQL(createIndexSql)
                            }
                        }
                    }
                }

                val views = db.optJSONArray("views")
                if (views != null) {
                    forEachObject(views) { view ->
                        val viewName = view.optString("viewName")
                        val createViewSql = view.optString("createSql")
                        if (createViewSql.isNullOrBlank().not()) {
                            database.execSQL(createViewSql.replace(ROOM_VIEW_NAME_PLACEHOLDER, viewName))
                        }
                    }
                }

                val setupQueries = db.getJSONArray("setupQueries")
                for (i in 0 until setupQueries.length()) {
                    database.execSQL(setupQueries.getString(i))
                }

                database.version = version
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
    }

    private fun forEachObject(
        array: JSONArray,
        block: (JSONObject) -> Unit
    ) {
        for (i in 0 until array.length()) {
            block(array.getJSONObject(i))
        }
    }

    private fun findProjectRoot(startDir: File): File? {
        var dir: File? = startDir
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile
        }
        return null
    }

    private fun deleteDatabaseFiles(
        context: Context,
        name: String
    ) {
        val dbFile = context.getDatabasePath(name)
        dbFile.delete()
        File(dbFile.absolutePath + "-shm").delete()
        File(dbFile.absolutePath + "-wal").delete()
    }

    private companion object {
        const val OLD_VERSION = 15
        const val NEW_VERSION = 16
        const val TEST_DB_NAME = "room_migration_gate_test.db"
        private const val ROOM_TABLE_NAME_PLACEHOLDER = "${'$'}{TABLE_NAME}"
        private const val ROOM_VIEW_NAME_PLACEHOLDER = "${'$'}{VIEW_NAME}"
    }
}
