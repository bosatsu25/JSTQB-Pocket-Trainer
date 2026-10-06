package jp.co.testreason.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `question_completions` (
                `sessionId` TEXT NOT NULL,
                `questionId` TEXT NOT NULL,
                `completedAt` INTEGER NOT NULL,
                PRIMARY KEY(`sessionId`, `questionId`)
            )
            """.trimIndent()
        )
    }
}
