package jp.co.testreason.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TestReasonDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration1To2_createsQuestionCompletionsTableAndPreservesV1Data() {
        // 1. Create DB at schema version 1 and insert v1 fixture via SQL
        val v1Db = helper.createDatabase("migration_test.db", 1)
        v1Db.execSQL(
            """
            INSERT INTO `quiz_sessions` (`id`, `mode`, `questionIds`, `currentQuestionIndex`, `startedAt`, `completedAt`, `isFinalized`)
            VALUES ('sess_mig_1', 'DAILY', '["q1","q2"]', 0, 1000, NULL, 0)
            """.trimIndent()
        )
        v1Db.close()

        // 2. Run Migration 1 -> 2 and validate schema v2
        val v2Db = helper.runMigrationsAndValidate("migration_test.db", 2, true, MIGRATION_1_2)

        // 3. Verify v1 data is preserved
        val cursor = v2Db.query("SELECT * FROM `quiz_sessions` WHERE `id` = 'sess_mig_1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("DAILY", cursor.getString(cursor.getColumnIndexOrThrow("mode")))
        cursor.close()

        // 4. Verify question_completions table exists and accepts inserts
        v2Db.execSQL(
            """
            INSERT INTO `question_completions` (`sessionId`, `questionId`, `completedAt`)
            VALUES ('sess_mig_1', 'q1', 1005)
            """.trimIndent()
        )
        val completionCursor = v2Db.query("SELECT * FROM `question_completions` WHERE `sessionId` = 'sess_mig_1'")
        assertTrue(completionCursor.moveToFirst())
        assertEquals("q1", completionCursor.getString(completionCursor.getColumnIndexOrThrow("questionId")))
        completionCursor.close()

        v2Db.close()
    }
}
