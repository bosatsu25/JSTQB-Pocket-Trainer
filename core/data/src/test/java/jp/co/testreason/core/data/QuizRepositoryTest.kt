package jp.co.testreason.core.data

import jp.co.testreason.core.database.AttemptEntity
import jp.co.testreason.core.database.QuestionDao
import jp.co.testreason.core.database.AttemptDao
import jp.co.testreason.core.database.SessionDao
import jp.co.testreason.core.model.ConfidenceLevel
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
}

class FakeQuestionDao : QuestionDao {
    private val questions = SampleQuestions.getInitialQuestions().map { it.toEntity() }.toMutableList()

    override fun getAllQuestions() = flowOf(questions)
    override suspend fun getQuestionById(id: String) = questions.find { it.id == id }
    override fun getQuestionsByLo(loId: String) = flowOf(questions.filter { it.learningObjectiveId == loId })
    override suspend fun getRandomQuestions(limit: Int) = questions.take(limit)
    override suspend fun insertQuestions(questions: List<jp.co.testreason.core.database.QuestionEntity>) {
        this.questions.addAll(questions)
    }
}

class FakeAttemptDao : AttemptDao {
    private val attempts = mutableListOf<AttemptEntity>()
    override fun getAttemptsBySession(sessionId: String) = flowOf(attempts.filter { it.sessionId == sessionId })
    override fun getAttemptsByQuestion(questionId: String) = flowOf(attempts.filter { it.questionId == questionId })
    override suspend fun insertAttempt(attempt: AttemptEntity) { attempts.add(attempt) }
}

class FakeSessionDao : SessionDao {
    private var session: jp.co.testreason.core.database.SessionEntity? = null
    override suspend fun getSessionById(id: String) = session?.takeIf { it.id == id }
    override fun getActiveSession() = flowOf(session?.takeIf { !it.isFinalized })
    override suspend fun insertOrUpdateSession(session: jp.co.testreason.core.database.SessionEntity) {
        this.session = session
    }
}
