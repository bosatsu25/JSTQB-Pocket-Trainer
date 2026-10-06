package jp.co.testreason.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.database.TestReasonDatabase
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoryRollbackTest {

    private lateinit var db: TestReasonDatabase
    private lateinit var quizRepository: QuizRepository
    private lateinit var fakeMasteryRepository: FakeMasteryRepository
    private lateinit var timeProvider: MutableTimeProvider
    private lateinit var spacedReviewEngine: SpacedReviewEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            TestReasonDatabase::class.java
        ).allowMainThreadQueries().build()

        fakeMasteryRepository = FakeMasteryRepository()
        timeProvider = MutableTimeProvider(1000000L)
        spacedReviewEngine = SpacedReviewEngine(timeProvider)

        quizRepository = QuizRepositoryImpl(
            db = db,
            masteryRepository = fakeMasteryRepository,
            spacedReviewEngine = spacedReviewEngine,
            timeProvider = timeProvider
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun finalizeAttempt_rollbackRevertsAttemptWhenScheduleFails() = runTest {
        val session = quizRepository.createDailySession(5)
        val firstQId = session.questionIds[0]

        // Inject SQLite trigger to force failure during ReviewSchedule insertion
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_review_schedule_insert
            BEFORE INSERT ON review_schedules
            BEGIN
                SELECT RAISE(ABORT, 'Forced trigger failure on review_schedules');
            END;
            """.trimIndent()
        )

        val attempt = Attempt(
            id = "att_rollback_1",
            sessionId = session.id,
            questionId = firstQId,
            selectedChoiceId = "A",
            isCorrect = false,
            confidence = ConfidenceLevel.LOW,
            mistakeReason = null,
            timeSpentMs = 1000,
            timestamp = 1000000L
        )

        try {
            // Direct call to production repository method
            quizRepository.finalizeAttempt(attempt)
            fail("Expected exception during finalizeAttempt transaction")
        } catch (e: Exception) {
            // Expected trigger error
        }

        // Verify production transaction rolled back BOTH attempt and review_schedule
        val savedAttempt = db.attemptDao().getAttempt(session.id, firstQId)
        assertNull("Attempt should be rolled back and null", savedAttempt)

        val savedSchedule = db.reviewScheduleDao().getSchedule(firstQId)
        assertNull("Schedule should be rolled back and null", savedSchedule)
    }

    @Test
    fun completeQuestion_rollbackRevertsCompletionAndSessionIndexWhenSessionUpdateFails() = runTest {
        val session = quizRepository.createDailySession(5)
        val firstQId = session.questionIds[0]

        val attempt = Attempt(
            id = "att_rollback_2",
            sessionId = session.id,
            questionId = firstQId,
            selectedChoiceId = "B",
            isCorrect = true,
            confidence = ConfidenceLevel.HIGH,
            mistakeReason = null,
            timeSpentMs = 1000,
            timestamp = 1000000L
        )
        quizRepository.finalizeAttempt(attempt)

        // Inject SQLite trigger on BEFORE INSERT ON quiz_sessions (fired by REPLACE)
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_session_insert
            BEFORE INSERT ON quiz_sessions
            BEGIN
                SELECT RAISE(ABORT, 'Forced trigger failure on quiz_sessions insert/replace');
            END;
            """.trimIndent()
        )

        try {
            // Direct call to production repository method
            quizRepository.completeQuestion(session.id, firstQId)
            fail("Expected exception during completeQuestion transaction")
        } catch (e: Exception) {
            // Expected trigger error
        }

        // Verify production transaction rolled back question_completion and session index
        val completion = db.questionCompletionDao().getCompletion(session.id, firstQId)
        assertNull("Completion should be rolled back and null", completion)

        val restoredSession = db.sessionDao().getSessionById(session.id)
        assertEquals(0, restoredSession?.currentQuestionIndex)
    }
}
