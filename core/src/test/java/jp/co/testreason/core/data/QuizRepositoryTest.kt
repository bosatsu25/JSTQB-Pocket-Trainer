package jp.co.testreason.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.database.ReviewScheduleEntity
import jp.co.testreason.core.database.TestReasonDatabase
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.Mastery
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.StudyMode
import jp.co.testreason.core.model.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuizRepositoryTest {

    private lateinit var db: TestReasonDatabase
    private lateinit var quizRepository: QuizRepository
    private lateinit var fakeMasteryRepository: FakeMasteryRepository
    private lateinit var testTimeProvider: MutableTimeProvider
    private lateinit var spacedReviewEngine: SpacedReviewEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            TestReasonDatabase::class.java
        ).allowMainThreadQueries().build()

        fakeMasteryRepository = FakeMasteryRepository()
        testTimeProvider = MutableTimeProvider(1000000L)
        spacedReviewEngine = SpacedReviewEngine(testTimeProvider)

        quizRepository = QuizRepositoryImpl(
            db = db,
            masteryRepository = fakeMasteryRepository,
            spacedReviewEngine = spacedReviewEngine,
            timeProvider = testTimeProvider
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createDailySession_seedsQuestionsAndCreatesSession() = runTest {
        val session = quizRepository.createDailySession(5)

        assertNotNull(session)
        assertEquals(StudyMode.DAILY, session.mode)
        assertEquals(5, session.questionIds.size)
        assertEquals(0, session.currentQuestionIndex)
    }

    @Test
    fun finalizeAttempt_resubmissionPreservesFirstAnswerAndChoice() = runTest {
        val session = quizRepository.createDailySession(5)
        val firstQId = session.questionIds[0]

        val initialAttempt = Attempt(
            id = "att1",
            sessionId = session.id,
            questionId = firstQId,
            selectedChoiceId = "A",
            isCorrect = false,
            confidence = ConfidenceLevel.LOW,
            mistakeReason = null,
            timeSpentMs = 1000,
            timestamp = 100000
        )
        val firstResult = quizRepository.finalizeAttempt(initialAttempt)
        assertTrue(firstResult)

        val duplicateSubmission = Attempt(
            id = "att2",
            sessionId = session.id,
            questionId = firstQId,
            selectedChoiceId = "B",
            isCorrect = true,
            confidence = ConfidenceLevel.HIGH,
            mistakeReason = MistakeReason.MISREAD,
            timeSpentMs = 2000,
            timestamp = 100050
        )
        val secondResult = quizRepository.finalizeAttempt(duplicateSubmission)
        assertFalse(secondResult) // Rejected!

        val attempts = quizRepository.getAttemptsForSession(session.id).first()
        assertEquals(1, attempts.size)
        assertEquals("A", attempts[0].selectedChoiceId) // First choice preserved!
        assertEquals(false, attempts[0].isCorrect)
    }

    @Test
    fun observeDueReviewQuestions_includesDueAndNow_excludesFutureAndUnseen() = runTest {
        quizRepository.seedInitialQuestionsIfEmpty()
        // Current time: 1000000L
        val dueSchedule = ReviewScheduleEntity(
            questionId = "q_fl_1_1_1",
            nextReviewAt = 500000L, // Past (< now)
            lastAttemptAt = 100000L,
            intervalDays = 1
        )
        val nowSchedule = ReviewScheduleEntity(
            questionId = "q_fl_1_2_1",
            nextReviewAt = 1000000L, // Equal to now
            lastAttemptAt = 100000L,
            intervalDays = 1
        )
        val futureSchedule = ReviewScheduleEntity(
            questionId = "q_fl_2_1_1",
            nextReviewAt = 2000000L, // Future relative to 1000000L (> now)
            lastAttemptAt = 100000L,
            intervalDays = 7
        )
        // q_fl_2_2_1 has no schedule record (q_unseen)

        db.reviewScheduleDao().insertOrUpdateSchedule(dueSchedule)
        db.reviewScheduleDao().insertOrUpdateSchedule(nowSchedule)
        db.reviewScheduleDao().insertOrUpdateSchedule(futureSchedule)

        val dueQuestions = quizRepository.observeDueReviewQuestions().first()
        assertEquals(2, dueQuestions.size)
        assertTrue(dueQuestions.any { it.id == "q_fl_1_1_1" })
        assertTrue(dueQuestions.any { it.id == "q_fl_1_2_1" })
        assertFalse(dueQuestions.any { it.id == "q_fl_2_1_1" }) // Future excluded!
        assertFalse(dueQuestions.any { it.id == "q_fl_2_2_1" }) // Unseen excluded!

        val dueSession = quizRepository.createDueReviewSession()
        assertEquals(StudyMode.REVIEW, dueSession.mode)
        assertEquals(2, dueSession.questionIds.size)
        assertTrue(dueSession.questionIds.contains("q_fl_1_1_1"))
        assertTrue(dueSession.questionIds.contains("q_fl_1_2_1"))
    }

    @Test
    fun completeQuestion_isIdempotentAndUpdatesMasteryOnce() = runTest {
        val session = quizRepository.createDailySession(5)
        val firstQId = session.questionIds[0]

        val attempt = Attempt(
            id = "att1",
            sessionId = session.id,
            questionId = firstQId,
            selectedChoiceId = "B",
            isCorrect = true,
            confidence = ConfidenceLevel.HIGH,
            mistakeReason = null,
            timeSpentMs = 1000,
            timestamp = testTimeProvider.currentTimeMillis()
        )
        quizRepository.finalizeAttempt(attempt)

        val updatedSession1 = quizRepository.completeQuestion(session.id, firstQId)
        assertEquals(1, updatedSession1?.currentQuestionIndex)
        assertEquals(1, fakeMasteryRepository.updateCount)

        // Repeat completeQuestion for same question - MUST NOT increment updateCount or index!
        val updatedSession2 = quizRepository.completeQuestion(session.id, firstQId)
        assertEquals(1, updatedSession2?.currentQuestionIndex)
        assertEquals(1, fakeMasteryRepository.updateCount) // Mastery updated ONLY ONCE!
    }

    @Test
    fun completeQuestion_preventsAdvancingToFutureQuestions() = runTest {
        val session = quizRepository.createDailySession(5)
        val thirdQId = session.questionIds[2] // Index 2 (future question)

        val attempt = Attempt(
            id = "att_future",
            sessionId = session.id,
            questionId = thirdQId,
            selectedChoiceId = "A",
            isCorrect = true,
            confidence = ConfidenceLevel.HIGH,
            mistakeReason = null,
            timeSpentMs = 1000,
            timestamp = testTimeProvider.currentTimeMillis()
        )
        // Finalizing attempt on future question is rejected by domain invariant
        val finalized = quizRepository.finalizeAttempt(attempt)
        assertFalse(finalized)

        // Attempting to complete future question leaves session index unchanged at 0
        val sessionAfterFuture = quizRepository.completeQuestion(session.id, thirdQId)
        assertEquals(0, sessionAfterFuture?.currentQuestionIndex)
        assertEquals(0, fakeMasteryRepository.updateCount)
    }
}

class MutableTimeProvider(var timeMs: Long) : TimeProvider {
    override fun currentTimeMillis(): Long = timeMs
}

class FakeMasteryRepository : MasteryRepository {
    var updateCount = 0
    private val masteries = mutableMapOf<String, Mastery>()

    override fun getAllMasteries() = flowOf(masteries.values.toList())
    override suspend fun updateMastery(learningObjectiveId: String, isCorrect: Boolean) {
        updateCount++
        masteries[learningObjectiveId] = Mastery(learningObjectiveId, if (isCorrect) 1f else 0f, 1000L, 1, if (isCorrect) 1 else 0)
    }
}
