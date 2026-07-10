package com.example.interviewassistant.core.database

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
        }
        return driver
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
