package jp.co.testreason.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.database.AttemptEntity
import jp.co.testreason.core.database.ReviewScheduleEntity
import jp.co.testreason.core.database.TestReasonDatabase
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Issues #14 and #15: review scheduling, weakness detection and session creation. */
@RunWith(RobolectricTestRunner::class)
class ReviewIntelligenceTest {

    private lateinit var db: TestReasonDatabase
    private lateinit var repository: QuizRepository
    private val time = MutableTimeProvider(NOW)

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TestReasonDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = QuizRepositoryImpl(
            db = db,
            masteryRepository = FakeMasteryRepository(),
            spacedReviewEngine = SpacedReviewEngine(time),
            timeProvider = time,
            bundledQuestionSource = testQuestionSource
        )
        repository.syncBundledQuestions()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun attempt(sessionId: String, questionId: String, correct: Boolean, confidence: ConfidenceLevel?, at: Long) =
        AttemptEntity("a_${sessionId}_$questionId", sessionId, questionId, "A", correct, confidence, null, 1_000L, at)

    private suspend fun answerCurrentWrong(sessionId: String): String {
        val session = checkNotNull(repository.getSessionById(sessionId))
        val questionId = session.questionIds[session.currentQuestionIndex]
        val accepted = repository.finalizeAttempt(
            Attempt("att_$questionId", sessionId, questionId, listOf("A"), false, ConfidenceLevel.HIGH, null, 1_000L, time.timeMs)
        )
        assertTrue(accepted)
        return questionId
    }

    @Test
    fun mistakeReason_decidesTheNextReviewInterval() = runTest {
        val session = checkNotNull(repository.createDailySession(5))

        val careless = answerCurrentWrong(session.id)
        repository.updateMistakeReason(session.id, careless, MistakeReason.CARELESS)
        assertEquals(3, db.reviewScheduleDao().getSchedule(careless)!!.intervalDays)

        repository.completeQuestion(session.id, careless)
        val concept = answerCurrentWrong(session.id)
        repository.updateMistakeReason(session.id, concept, MistakeReason.CONCEPT)
        assertEquals(1, db.reviewScheduleDao().getSchedule(concept)!!.intervalDays)
    }

    @Test
    fun weakQuestion_isClearedByALaterConfidentCorrectAnswer() = runTest {
        db.attemptDao().insertAttemptIgnore(attempt("s1", "q_fl_1_1_1", correct = false, confidence = ConfidenceLevel.HIGH, at = 100L))
        db.attemptDao().insertAttemptIgnore(attempt("s1", "q_fl_2_1_1", correct = true, confidence = ConfidenceLevel.GUESS, at = 100L))
        assertEquals(setOf("q_fl_1_1_1", "q_fl_2_1_1"), repository.observeWeakQuestions().first().map { it.id }.toSet())

        db.attemptDao().insertAttemptIgnore(attempt("s2", "q_fl_1_1_1", correct = true, confidence = ConfidenceLevel.HIGH, at = 200L))

        assertEquals(listOf("q_fl_2_1_1"), repository.observeWeakQuestions().first().map { it.id })
    }

    @Test
    fun emptyReviewAndWeaknessQueues_doNotCreateSessions() = runTest {
        assertNull(repository.createDueReviewSession())
        assertNull(repository.createWeaknessSession())
        assertNull(repository.getActiveSession().first())
    }

    @Test
    fun dueQueue_followsTheClockWithoutDataChanges() = runTest {
        db.reviewScheduleDao().insertOrUpdateSchedule(ReviewScheduleEntity("q_fl_1_1_1", NOW + 1_000L, 0L, 1))
        val due = repository.observeDueReviewQuestions()
        assertTrue(due.first().isEmpty())

        time.timeMs = NOW + 2_000L

        assertEquals(listOf("q_fl_1_1_1"), due.first().map { it.id })
    }

    @Test
    fun dailySession_putsDueThenWeakThenUnseenQuestions() = runTest {
        db.reviewScheduleDao().insertOrUpdateSchedule(ReviewScheduleEntity("q_fl_3_1_1", NOW - 1L, 0L, 1))
        db.attemptDao().insertAttemptIgnore(attempt("s0", "q_fl_3_1_1", correct = true, confidence = ConfidenceLevel.HIGH, at = 10L))
        db.reviewScheduleDao().insertOrUpdateSchedule(ReviewScheduleEntity("q_fl_2_2_1", NOW + DAY, 0L, 1))
        db.attemptDao().insertAttemptIgnore(attempt("s0", "q_fl_2_2_1", correct = false, confidence = ConfidenceLevel.HIGH, at = 10L))

        val session = checkNotNull(repository.createDailySession(3))

        assertEquals(listOf("q_fl_3_1_1", "q_fl_2_2_1"), session.questionIds.take(2))
        assertFalse(session.questionIds[2] in setOf("q_fl_3_1_1", "q_fl_2_2_1"))
        assertEquals(3, session.questionIds.toSet().size)
    }

    private companion object {
        const val NOW = 10_000_000L
        const val DAY = 24 * 3600 * 1000L
    }
}
