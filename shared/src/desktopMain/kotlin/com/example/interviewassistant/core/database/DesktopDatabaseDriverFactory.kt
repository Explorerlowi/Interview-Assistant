package com.example.interviewassistant.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.interviewassistant.database.InterviewDatabase
import java.nio.file.Files
import java.nio.file.Path

/**
 * Creates the Windows desktop SQLite database under the current user's app-data directory.
 */
class DesktopDatabaseDriverFactory : DatabaseDriverFactory {
    override fun create(): SqlDriver {
        val directory = applicationDataDirectory()
        Files.createDirectories(directory)
        val databasePath = directory.resolve(DATABASE_NAME)
        val isNewDatabase = Files.notExists(databasePath)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.toAbsolutePath()}")
        if (isNewDatabase) {
            InterviewDatabase.Schema.create(driver)
        } else {
            migrateIfNeeded(driver)
        }
        return driver
    }

    /**
     * Applies SQLDelight migrations for databases created by earlier app versions.
     *
     * Legacy installs never stamped `user_version`, so version `0` is treated as schema v1.
     */
    private fun migrateIfNeeded(driver: SqlDriver) {
        val currentVersion = readUserVersion(driver).coerceAtLeast(1)
        val targetVersion = InterviewDatabase.Schema.version
        if (currentVersion < targetVersion) {
            InterviewDatabase.Schema.migrate(driver, currentVersion, targetVersion)
        }
    }

    private fun readUserVersion(driver: SqlDriver): Long {
        return driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
            },
            parameters = 0,
        ).value
    }

    private fun applicationDataDirectory(): Path {
        val root = System.getenv("APPDATA")
            ?.takeIf(String::isNotBlank)
            ?: System.getProperty("user.home")
        return Path.of(root, "InterviewAssistant")
    }

    private companion object {
        const val DATABASE_NAME = "interview-assistant.db"
    }
}
