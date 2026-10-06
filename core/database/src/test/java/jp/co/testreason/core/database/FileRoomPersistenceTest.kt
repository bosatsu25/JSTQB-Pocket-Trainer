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
        context.deleteDatabase("file_persistence_test.db")
    }

    @After
    fun tearDown() {
        context.deleteDatabase("file_persistence_test.db")
    }

    @Test
    fun fileDatabase_savesAndRestoresStateAcrossDbReopen() = runTest {
        // 1. Open DB, insert data, then close
        var db1: TestReasonDatabase? = Room.databaseBuilder(
            context,
            TestReasonDatabase::class.java,
            "file_persistence_test.db"
        ).allowMainThreadQueries().build()

        val session1 = SessionEntity(
            id = "s_file_1",
            mode = StudyMode.DAILY,
            questionIds = listOf("q1", "q2"),
            currentQuestionIndex = 1,
            startedAt = 50000,
            completedAt = null,
            isFinalized = false
        )
        db1!!.sessionDao().insertOrUpdateSession(session1)

        val attempt1 = AttemptEntity(
            id = "att_file_1",
            sessionId = "s_file_1",
            questionId = "q1",
            selectedChoiceId = "B",
            isCorrect = true,
            confidence = ConfidenceLevel.HIGH,
            mistakeReason = null,
            timeSpentMs = 1200,
            timestamp = 50005
        )
        db1.attemptDao().insertAttemptIgnore(attempt1)

        val schedule1 = ReviewScheduleEntity(
            questionId = "q1",
            nextReviewAt = 50000 + 7 * 86400000L,
            lastAttemptAt = 50005,
            intervalDays = 7
        )
        db1.reviewScheduleDao().insertOrUpdateSchedule(schedule1)

        db1.close()
        db1 = null

        // 2. Reopen DB from file and verify persistence
        val db2 = Room.databaseBuilder(
            context,
            TestReasonDatabase::class.java,
            "file_persistence_test.db"
        ).allowMainThreadQueries().build()

        val restoredSession = db2.sessionDao().getSessionById("s_file_1")
        assertNotNull(restoredSession)
        assertEquals(1, restoredSession?.currentQuestionIndex)

        val restoredAttempts = db2.attemptDao().getAttemptsBySession("s_file_1").first()
        assertEquals(1, restoredAttempts.size)
        assertEquals("B", restoredAttempts[0].selectedChoiceId)

        val restoredSchedule = db2.reviewScheduleDao().getSchedule("q1")
        assertNotNull(restoredSchedule)
        assertEquals(7, restoredSchedule?.intervalDays)

        db2.close()
    }
}
