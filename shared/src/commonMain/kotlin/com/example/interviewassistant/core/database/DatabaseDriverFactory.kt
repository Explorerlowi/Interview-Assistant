package com.example.interviewassistant.core.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Creates the platform SQLDelight driver used by the shared repositories.
 */
interface DatabaseDriverFactory {
    /** Creates a driver for the application database. */
    fun create(): SqlDriver
}
