package jp.co.testreason.core.data

import jp.co.testreason.core.database.*
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuizRepositoryTest {

    private lateinit var quizRepository: QuizRepository
    private lateinit var fakeQuestionDao: FakeQuestionDao
    private lateinit var fakeAttemptDao: FakeAttemptDao
    private lateinit var fakeSessionDao: FakeSessionDao
    private lateinit var fakeReviewScheduleDao: FakeReviewScheduleDao
    private lateinit var fakeQuestionCompletionDao: FakeQuestionCompletionDao
    private lateinit var fakeMasteryRepository: FakeMasteryRepository
    private lateinit var testTimeProvider: MutableTimeProvider
    private lateinit var spacedReviewEngine: SpacedReviewEngine

    @Before
    fun setUp() {
        fakeQuestionDao = FakeQuestionDao()
        fakeAttemptDao = FakeAttemptDao()
        fakeSessionDao = FakeSessionDao()
        fakeReviewScheduleDao = FakeReviewScheduleDao()
        fakeQuestionCompletionDao = FakeQuestionCompletionDao()
        fakeMasteryRepository = FakeMasteryRepository()
        testTimeProvider = MutableTimeProvider(1000000L)
        spacedReviewEngine = SpacedReviewEngine(testTimeProvider)

        quizRepository = QuizRepositoryImpl(
            questionDao = fakeQuestionDao,
            attemptDao = fakeAttemptDao,
            sessionDao = fakeSessionDao,
            reviewScheduleDao = fakeReviewScheduleDao,
            questionCompletionDao = fakeQuestionCompletionDao,
            masteryRepository = fakeMasteryRepository,
            spacedReviewEngine = spacedReviewEngine,
            timeProvider = testTimeProvider
        )
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
        val initialAttempt = Attempt(
            id = "att1",
            sessionId = "s1",
            questionId = "q_fl_1_1_1",
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
            sessionId = "s1",
            questionId = "q_fl_1_1_1",
            selectedChoiceId = "B",
            isCorrect = true,
            confidence = ConfidenceLevel.HIGH,
            mistakeReason = MistakeReason.MISREAD,
            timeSpentMs = 2000,
            timestamp = 100050
        )
        val secondResult = quizRepository.finalizeAttempt(duplicateSubmission)
        assertFalse(secondResult) // Rejected!

        val attempts = quizRepository.getAttemptsForSession("s1").first()
        assertEquals(1, attempts.size)
        assertEquals("A", attempts[0].selectedChoiceId) // First choice preserved!
        assertEquals(false, attempts[0].isCorrect)
    }

    @Test
    fun observeDueReviewQuestions_filtersOnlyDueQuestions() = runTest {
        val dueSchedule = ReviewScheduleEntity(
            questionId = "q_fl_1_1_1",
            nextReviewAt = 500000L, // Past
            lastAttemptAt = 100000L,
            intervalDays = 1
        )
        val nowSchedule = ReviewScheduleEntity(
            questionId = "q_fl_1_2_1",
            nextReviewAt = 1000000L, // Equal to current time
            lastAttemptAt = 100000L,
            intervalDays = 1
        )
        val futureSchedule = ReviewScheduleEntity(
            questionId = "q_fl_2_1_1",
            nextReviewAt = 2000000L, // Future relative to 1000000L
            lastAttemptAt = 100000L,
            intervalDays = 7
        )
        fakeReviewScheduleDao.insertOrUpdateSchedule(dueSchedule)
        fakeReviewScheduleDao.insertOrUpdateSchedule(nowSchedule)
        fakeReviewScheduleDao.insertOrUpdateSchedule(futureSchedule)

        val dueQuestions = quizRepository.observeDueReviewQuestions().first()
        assertEquals(2, dueQuestions.size)
        assertTrue(dueQuestions.any { it.id == "q_fl_1_1_1" })
        assertTrue(dueQuestions.any { it.id == "q_fl_1_2_1" })
        assertFalse(dueQuestions.any { it.id == "q_fl_2_1_1" }) // Future excluded!
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
}

class MutableTimeProvider(var timeMs: Long) : TimeProvider {
    override fun currentTimeMillis(): Long = timeMs
}

class FakeQuestionDao : QuestionDao {
    private val questions = SampleQuestions.getInitialQuestions().map { it.toEntity() }.toMutableList()

    override fun getAllQuestions() = flowOf(questions)
    override suspend fun getQuestionById(id: String) = questions.find { it.id == id }
    override fun getQuestionsByLo(loId: String) = flowOf(questions.filter { it.learningObjectiveId == loId })
    override suspend fun getRandomQuestions(limit: Int) = questions.take(limit)
    override suspend fun insertQuestions(questions: List<QuestionEntity>) {
        this.questions.addAll(questions)
    }
}

class FakeAttemptDao : AttemptDao {
    private val attempts = mutableListOf<AttemptEntity>()
    override fun getAttemptsBySession(sessionId: String) = flowOf(attempts.filter { it.sessionId == sessionId })
    override fun getAttemptsByQuestion(questionId: String) = flowOf(attempts.filter { it.questionId == questionId })
    override suspend fun getAttempt(sessionId: String, questionId: String) =
        attempts.find { it.sessionId == sessionId && it.questionId == questionId }
    override fun getAllAttempts() = flowOf(attempts)
    override suspend fun insertAttemptIgnore(attempt: AttemptEntity): Long {
        val exists = attempts.any { it.sessionId == attempt.sessionId && it.questionId == attempt.questionId }
        if (exists) return -1L
        attempts.add(attempt)
        return attempts.size.toLong()
    }
    override suspend fun updateMistakeReasonOnly(sessionId: String, questionId: String, mistakeReason: MistakeReason) {
        val index = attempts.indexOfFirst { it.sessionId == sessionId && it.questionId == questionId }
        if (index >= 0) {
            attempts[index] = attempts[index].copy(mistakeReason = mistakeReason)
        }
    }
}

class FakeSessionDao : SessionDao {
    private var session: SessionEntity? = null
    override suspend fun getSessionById(id: String) = session?.takeIf { it.id == id }
    override fun getActiveSession() = flowOf(session?.takeIf { !it.isFinalized })
    override suspend fun insertOrUpdateSession(session: SessionEntity) {
        this.session = session
    }
}

class FakeReviewScheduleDao : ReviewScheduleDao {
    private val schedules = mutableMapOf<String, ReviewScheduleEntity>()

    override fun getDueReviewSchedules(now: Long) = flowOf(schedules.values.filter { it.nextReviewAt <= now })
    override suspend fun getSchedule(questionId: String) = schedules[questionId]
    override fun getAllSchedules() = flowOf(schedules.values.toList())
    override suspend fun insertOrUpdateSchedule(schedule: ReviewScheduleEntity) {
        schedules[schedule.questionId] = schedule
    }
}

class FakeQuestionCompletionDao : QuestionCompletionDao {
    private val completions = mutableListOf<QuestionCompletionEntity>()

    override suspend fun getCompletion(sessionId: String, questionId: String): QuestionCompletionEntity? =
        completions.find { it.sessionId == sessionId && it.questionId == questionId }

    override suspend fun insertCompletionIgnore(completion: QuestionCompletionEntity): Long {
        val exists = completions.any { it.sessionId == completion.sessionId && it.questionId == completion.questionId }
        if (exists) return -1L
        completions.add(completion)
        return completions.size.toLong()
    }
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
