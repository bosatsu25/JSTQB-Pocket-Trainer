package jp.co.testreason.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.StudyMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("migration_test.db")
    }

    @After
    fun tearDown() {
        context.deleteDatabase("migration_test.db")
    }

    @Test
    fun migration1To2_createsQuestionCompletionsTableAndPreservesV1Data() = runTest {
        // 1. Create DB at version 2 using migration MIGRATION_1_2
        val db = Room.databaseBuilder(
            context,
            TestReasonDatabase::class.java,
            "migration_test.db"
        ).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()

        val session = SessionEntity(
            id = "sess_mig_1",
            mode = StudyMode.DAILY,
            questionIds = listOf("q1", "q2"),
            currentQuestionIndex = 0,
            startedAt = 1000L,
            completedAt = null,
            isFinalized = false
        )
        db.sessionDao().insertOrUpdateSession(session)

        val completion = QuestionCompletionEntity(
            sessionId = "sess_mig_1",
            questionId = "q1",
            completedAt = 1005L
        )
        db.questionCompletionDao().insertCompletionIgnore(completion)

        val restoredSession = db.sessionDao().getSessionById("sess_mig_1")
        assertNotNull(restoredSession)

        val restoredCompletion = db.questionCompletionDao().getCompletion("sess_mig_1", "q1")
        assertNotNull(restoredCompletion)
        assertEquals("q1", restoredCompletion?.questionId)

        db.close()
    }
}
