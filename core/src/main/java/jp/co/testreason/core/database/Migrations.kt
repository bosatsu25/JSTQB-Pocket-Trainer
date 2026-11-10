package jp.co.testreason.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2: question format extension (multi-answer, scenario, content revision, retirement). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE questions ADD COLUMN selectCount INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE questions ADD COLUMN scenario TEXT")
        db.execSQL("ALTER TABLE questions ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE questions ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
    }
}

/** v2 -> v3: mock exam tables (#19). Only adds tables; existing data is untouched. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `mock_exams` (`sessionId` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, " +
                "`submittedAt` INTEGER, PRIMARY KEY(`sessionId`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exam_answers` (`sessionId` TEXT NOT NULL, `questionId` TEXT NOT NULL, " +
                "`selectedChoiceIds` TEXT NOT NULL, `flagged` INTEGER NOT NULL, PRIMARY KEY(`sessionId`, `questionId`))"
        )
    }
}
