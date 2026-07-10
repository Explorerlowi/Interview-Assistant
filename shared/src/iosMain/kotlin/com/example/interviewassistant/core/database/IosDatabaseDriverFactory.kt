package com.example.interviewassistant.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.interviewassistant.database.InterviewDatabase

/**
 * Keeps the existing iOS target compilable while product support remains out of scope.
 */
class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun create(): SqlDriver {
        return NativeSqliteDriver(InterviewDatabase.Schema, "interview-assistant.db")
    }
}
