package com.example.interviewassistant.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.interviewassistant.database.InterviewDatabase

/**
 * Creates the Android SQLite database in application-private storage.
 */
class AndroidDatabaseDriverFactory(
    private val context: Context,
) : DatabaseDriverFactory {
    override fun create(): SqlDriver {
        return AndroidSqliteDriver(
            schema = InterviewDatabase.Schema,
            context = context,
            name = DATABASE_NAME,
        )
    }

    private companion object {
        const val DATABASE_NAME = "interview-assistant.db"
    }
}
