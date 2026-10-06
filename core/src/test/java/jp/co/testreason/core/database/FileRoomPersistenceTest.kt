package jp.co.testreason.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.StudyMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileRoomPersistenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("file_persistence_full_test.db")
    }

    @After
    fun tearDown() {
        context.deleteDatabase("file_persistence_full_test.db")
    }

    @Test
    fun fileDatabase_savesAndRestoresCompleteStateAcrossDbReopen() = runTest {
        // 1. Open DB, insert unfinalized and finalized sessions, attempts, completions, schedules, then close
        var db1: TestReasonDatabase? = Room.databaseBuilder(
            context,
            TestReasonDatabase::class.java,
            "file_persistence_full_test.db"
        ).allowMainThreadQueries().build()

        val questionOrder = listOf("q_fl_1_1_1", "q_fl_1_2_1", "q_fl_2_1_1")

        val activeSession = SessionEntity(
            id = "s_active_1",
            mode = StudyMode.DAILY,
            questionIds = questionOrder,
            currentQuestionIndex = 2,
            startedAt = 100000L,
            completedAt = null,
            isFinalized = false
        )
        db1!!.sessionDao().insertOrUpdateSession(activeSession)

        val completedSession = SessionEntity(
            id = "s_completed_1",
            mode = StudyMode.WEAKNESS,
            questionIds = listOf("q_fl_1_1_1"),
            currentQuestionIndex = 1,
            startedAt = 50000L,
            completedAt = 90000L,
            isFinalized = true
        )
        db1.sessionDao().insertOrUpdateSession(completedSession)

        val attempt1 = AttemptEntity(
            id = "att_file_1",
            sessionId = "s_active_1",
            questionId = "q_fl_1_1_1",
            selectedChoiceId = "B",
            isCorrect = true,
            confidence = ConfidenceLevel.HIGH,
            mistakeReason = null,
            timeSpentMs = 1200,
            timestamp = 100050L
        )
        db1.attemptDao().insertAttemptIgnore(attempt1)

        val completion1 = QuestionCompletionEntity(
            sessionId = "s_active_1",
            questionId = "q_fl_1_1_1",
            completedAt = 100055L
        )
        db1.questionCompletionDao().insertCompletionIgnore(completion1)

        val schedule1 = ReviewScheduleEntity(
            questionId = "q_fl_1_1_1",
            nextReviewAt = 100000L + 7 * 86400000L,
            lastAttemptAt = 100050L,
            intervalDays = 7
        )
        db1.reviewScheduleDao().insertOrUpdateSchedule(schedule1)

        db1.close()
        db1 = null

        // 2. Reopen DB from file and verify 100% complete state persistence
        val db2 = Room.databaseBuilder(
            context,
            TestReasonDatabase::class.java,
            "file_persistence_full_test.db"
        ).allowMainThreadQueries().build()

        // Verify active session & question order
        val restoredActive = db2.sessionDao().getSessionById("s_active_1")
        assertNotNull(restoredActive)
        assertEquals(2, restoredActive?.currentQuestionIndex)
        assertEquals(questionOrder, restoredActive?.questionIds)
        assertEquals(false, restoredActive?.isFinalized)

        // Verify completed session
        val restoredCompleted = db2.sessionDao().getSessionById("s_completed_1")
        assertNotNull(restoredCompleted)
        assertEquals(true, restoredCompleted?.isFinalized)
        assertEquals(90000L, restoredCompleted?.completedAt)

        // Verify attempt
        val restoredAttempt = db2.attemptDao().getAttempt("s_active_1", "q_fl_1_1_1")
        assertNotNull(restoredAttempt)
        assertEquals("B", restoredAttempt?.selectedChoiceId)
        assertEquals(true, restoredAttempt?.isCorrect)
        assertEquals(ConfidenceLevel.HIGH, restoredAttempt?.confidence)

        // Verify completion
        val restoredCompletion = db2.questionCompletionDao().getCompletion("s_active_1", "q_fl_1_1_1")
        assertNotNull(restoredCompletion)
        assertEquals(100055L, restoredCompletion?.completedAt)

        // Verify schedule
        val restoredSchedule = db2.reviewScheduleDao().getSchedule("q_fl_1_1_1")
        assertNotNull(restoredSchedule)
        assertEquals(7, restoredSchedule?.intervalDays)

        db2.close()
    }
}
