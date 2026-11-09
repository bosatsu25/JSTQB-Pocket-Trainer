package jp.co.testreason.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TestReasonDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_keepsQuestionsAndAttemptsAndAddsDefaults() = runTest {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO questions (id, learningObjectiveId, stem, choices, explanation) " +
                    "VALUES ('q1', 'FL-1.1.1', 'stem', '[]', 'exp')"
            )
            execSQL(
                "INSERT INTO attempts (id, sessionId, questionId, selectedChoiceId, isCorrect, confidence, " +
                    "mistakeReason, timeSpentMs, timestamp) VALUES ('a1', 's1', 'q1', 'B', 1, NULL, NULL, 10, 20)"
            )
            close()
        }

        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2).close()

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TestReasonDatabase::class.java,
            dbName
        ).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val question = db.questionDao().getQuestionById("q1")!!
            assertEquals(1, question.selectCount)
            assertNull(question.scenario)
            assertEquals(0, question.revision)
            assertTrue(question.isActive)
            assertEquals("B", db.attemptDao().getAttempt("s1", "q1")!!.selectedChoiceId)
        } finally {
            db.close()
        }
    }
}
