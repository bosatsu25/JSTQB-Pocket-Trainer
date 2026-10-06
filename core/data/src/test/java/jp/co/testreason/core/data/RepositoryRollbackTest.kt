package jp.co.testreason.core.data

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.database.*
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.*
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
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            TestReasonDatabase::class.java
        ).allowMainThreadQueries().build()

        fakeMasteryRepository = FakeMasteryRepository()
        timeProvider = MutableTimeProvider(1000000L)
        spacedReviewEngine = SpacedReviewEngine(timeProvider)

        quizRepository = QuizRepositoryImpl(
            db = db,
            questionDao = db.questionDao(),
            attemptDao = db.attemptDao(),
            sessionDao = db.sessionDao(),
            reviewScheduleDao = db.reviewScheduleDao(),
            questionCompletionDao = db.questionCompletionDao(),
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
    fun finalizeAttempt_transactionRollsBackOnException() = runTest {
        val attempt = Attempt(
            id = "att_err",
            sessionId = "s_err",
            questionId = "q_err",
            selectedChoiceId = "A",
            isCorrect = false,
            confidence = ConfidenceLevel.LOW,
            mistakeReason = null,
            timeSpentMs = 1000,
            timestamp = 1000000L
        )

        try {
            db.withTransaction {
                quizRepository.finalizeAttempt(attempt)
                throw IllegalStateException("Simulated exception after finalizeAttempt call inside transaction")
            }
            fail("Expected exception was not thrown")
        } catch (e: IllegalStateException) {
            // Expected
        }

        // Verify that neither Attempt nor ReviewSchedule was committed
        val savedAttempt = db.attemptDao().getAttempt("s_err", "q_err")
        assertNull("Attempt should be rolled back and null", savedAttempt)

        val savedSchedule = db.reviewScheduleDao().getSchedule("q_err")
        assertNull("Schedule should be rolled back and null", savedSchedule)
    }

    @Test
    fun completeQuestion_transactionRollsBackOnException() = runTest {
        val session = quizRepository.createDailySession(5)
        val firstQId = session.questionIds[0]

        try {
            db.withTransaction {
                quizRepository.completeQuestion(session.id, firstQId)
                throw IllegalStateException("Simulated exception inside transaction during completeQuestion")
            }
            fail("Expected exception was not thrown")
        } catch (e: IllegalStateException) {
            // Expected
        }

        // Verify that QuestionCompletion was NOT committed and Session index was NOT advanced
        val completion = db.questionCompletionDao().getCompletion(session.id, firstQId)
        assertNull("Completion should be rolled back and null", completion)

        val restoredSession = db.sessionDao().getSessionById(session.id)
        assertEquals(0, restoredSession?.currentQuestionIndex)
    }
}
