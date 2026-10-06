package jp.co.testreason.core.data

import jp.co.testreason.core.database.AttemptDao
import jp.co.testreason.core.database.AttemptEntity
import jp.co.testreason.core.database.QuestionDao
import jp.co.testreason.core.database.QuestionEntity
import jp.co.testreason.core.database.SessionDao
import jp.co.testreason.core.database.SessionEntity
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.StudyMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class QuizRepositoryTest {

    private lateinit var quizRepository: QuizRepository
    private lateinit var fakeQuestionDao: FakeQuestionDao
    private lateinit var fakeAttemptDao: FakeAttemptDao
    private lateinit var fakeSessionDao: FakeSessionDao

    @Before
    fun setUp() {
        fakeQuestionDao = FakeQuestionDao()
        fakeAttemptDao = FakeAttemptDao()
        fakeSessionDao = FakeSessionDao()
        quizRepository = QuizRepositoryImpl(fakeQuestionDao, fakeAttemptDao, fakeSessionDao)
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
    fun recordAttempt_resubmissionPreservesFirstAnswerAndChoice() = runTest {
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
        quizRepository.recordAttempt(initialAttempt)

        val duplicateSubmission = Attempt(
            id = "att2",
            sessionId = "s1",
            questionId = "q_fl_1_1_1",
            selectedChoiceId = "B", // Different choice
            isCorrect = true,
            confidence = ConfidenceLevel.HIGH,
            mistakeReason = MistakeReason.MISREAD, // Tagging mistake
            timeSpentMs = 2000,
            timestamp = 100050
        )
        quizRepository.recordAttempt(duplicateSubmission)

        val attempts = quizRepository.getAttemptsForSession("s1").first()
        assertEquals(1, attempts.size)
        assertEquals("A", attempts[0].selectedChoiceId) // First choice preserved!
        assertEquals(false, attempts[0].isCorrect)
        assertEquals(MistakeReason.MISREAD, attempts[0].mistakeReason) // Mistake tag updated!
    }

    @Test
    fun getReviewQuestions_filtersOnlyMistakesOrLowConfidence() = runTest {
        val incorrectAttempt = Attempt(
            id = "a1",
            sessionId = "s1",
            questionId = "q_fl_1_1_1",
            selectedChoiceId = "A",
            isCorrect = false,
            confidence = ConfidenceLevel.HIGH,
            mistakeReason = null,
            timeSpentMs = 1000,
            timestamp = 1000
        )
        quizRepository.recordAttempt(incorrectAttempt)

        val reviewQs = quizRepository.getReviewQuestions().first()
        assertEquals(1, reviewQs.size)
        assertEquals("q_fl_1_1_1", reviewQs[0].id)
    }
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
    override suspend fun insertAttempt(attempt: AttemptEntity) {
        val index = attempts.indexOfFirst { it.sessionId == attempt.sessionId && it.questionId == attempt.questionId }
        if (index >= 0) {
            attempts[index] = attempt
        } else {
            attempts.add(attempt)
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
