package jp.co.testreason.core.data

import jp.co.testreason.core.database.*
import jp.co.testreason.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface QuizRepository {
    fun getAllQuestions(): Flow<List<Question>>
    suspend fun getQuestionById(id: String): Question?
    suspend fun seedInitialQuestionsIfEmpty()
    suspend fun createDailySession(count: Int = 5): QuizSession
    suspend fun createWeaknessSession(): QuizSession
    suspend fun getSessionById(id: String): QuizSession?
    fun getActiveSession(): Flow<QuizSession?>
    suspend fun recordAttempt(attempt: Attempt)
    fun getAttemptsForSession(sessionId: String): Flow<List<Attempt>>
    fun getReviewQuestions(): Flow<List<Question>>
    suspend fun updateSessionProgress(sessionId: String, currentIndex: Int, isFinalized: Boolean)
}

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val questionDao: QuestionDao,
    private val attemptDao: AttemptDao,
    private val sessionDao: SessionDao
) : QuizRepository {

    override fun getAllQuestions(): Flow<List<Question>> {
        return questionDao.getAllQuestions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getQuestionById(id: String): Question? {
        return questionDao.getQuestionById(id)?.toDomain()
    }

    override suspend fun seedInitialQuestionsIfEmpty() {
        val existing = questionDao.getRandomQuestions(1)
        if (existing.isEmpty()) {
            questionDao.insertQuestions(SampleQuestions.getInitialQuestions().map { it.toEntity() })
        }
    }

    override suspend fun createDailySession(count: Int): QuizSession {
        seedInitialQuestionsIfEmpty()
        val questions = questionDao.getRandomQuestions(count)
        val session = QuizSession(
            id = "session_" + System.currentTimeMillis(),
            mode = StudyMode.DAILY,
            questionIds = questions.map { it.id },
            currentQuestionIndex = 0,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            isFinalized = false
        )
        sessionDao.insertOrUpdateSession(session.toEntity())
        return session
    }

    override suspend fun createWeaknessSession(): QuizSession {
        seedInitialQuestionsIfEmpty()
        val reviewQuestionsList = getReviewQuestions().first()
        val sessionQuestions = if (reviewQuestionsList.isNotEmpty()) {
            reviewQuestionsList.take(5)
        } else {
            emptyList()
        }
        val session = QuizSession(
            id = "session_weakness_" + System.currentTimeMillis(),
            mode = StudyMode.WEAKNESS,
            questionIds = sessionQuestions.map { it.id },
            currentQuestionIndex = 0,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            isFinalized = false
        )
        sessionDao.insertOrUpdateSession(session.toEntity())
        return session
    }

    override suspend fun getSessionById(id: String): QuizSession? {
        return sessionDao.getSessionById(id)?.toDomain()
    }

    override fun getActiveSession(): Flow<QuizSession?> {
        return sessionDao.getActiveSession().map { it?.toDomain() }
    }

    override suspend fun recordAttempt(attempt: Attempt) {
        val existing = attemptDao.getAttempt(attempt.sessionId, attempt.questionId)
        if (existing != null) {
            // Preserve initial choice and correctness; allow updating mistakeReason tag
            val updated = existing.copy(
                mistakeReason = attempt.mistakeReason ?: existing.mistakeReason
            )
            attemptDao.insertAttempt(updated)
        } else {
            attemptDao.insertAttempt(attempt.toEntity())
        }
    }

    override fun getAttemptsForSession(sessionId: String): Flow<List<Attempt>> {
        return attemptDao.getAttemptsBySession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getReviewQuestions(): Flow<List<Question>> {
        return attemptDao.getAllAttempts().map { attempts ->
            val reviewQuestionIds = attempts.filter { attempt ->
                !attempt.isCorrect ||
                        attempt.confidence == ConfidenceLevel.GUESS ||
                        attempt.confidence == ConfidenceLevel.LOW
            }.map { it.questionId }.distinct()

            reviewQuestionIds.mapNotNull { qId ->
                questionDao.getQuestionById(qId)?.toDomain()
            }
        }
    }

    override suspend fun updateSessionProgress(sessionId: String, currentIndex: Int, isFinalized: Boolean) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        // Do not allow rewinding index on finalized session
        val targetIndex = maxOf(session.currentQuestionIndex, currentIndex)
        val updated = session.copy(
            currentQuestionIndex = targetIndex,
            isFinalized = isFinalized || session.isFinalized,
            completedAt = if (isFinalized || session.isFinalized) (session.completedAt ?: System.currentTimeMillis()) else null
        )
        sessionDao.insertOrUpdateSession(updated)
    }
}

// Mappers
fun QuestionEntity.toDomain() = Question(
    id = id,
    learningObjectiveId = learningObjectiveId,
    stem = stem,
    choices = choices.map { Choice(it.id, it.text, it.isCorrect, it.distractorAnalysis) },
    explanation = explanation
)

fun Question.toEntity() = QuestionEntity(
    id = id,
    learningObjectiveId = learningObjectiveId,
    stem = stem,
    choices = choices.map { ChoiceEntity(it.id, it.text, it.isCorrect, it.distractorAnalysis) },
    explanation = explanation
)

fun AttemptEntity.toDomain() = Attempt(
    id = id,
    sessionId = sessionId,
    questionId = questionId,
    selectedChoiceId = selectedChoiceId,
    isCorrect = isCorrect,
    confidence = confidence,
    mistakeReason = mistakeReason,
    timeSpentMs = timeSpentMs,
    timestamp = timestamp
)

fun Attempt.toEntity() = AttemptEntity(
    id = id,
    sessionId = sessionId,
    questionId = questionId,
    selectedChoiceId = selectedChoiceId,
    isCorrect = isCorrect,
    confidence = confidence,
    mistakeReason = mistakeReason,
    timeSpentMs = timeSpentMs,
    timestamp = timestamp
)

fun SessionEntity.toDomain() = QuizSession(
    id = id,
    mode = mode,
    questionIds = questionIds,
    currentQuestionIndex = currentQuestionIndex,
    startedAt = startedAt,
    completedAt = completedAt,
    isFinalized = isFinalized
)

fun QuizSession.toEntity() = SessionEntity(
    id = id,
    mode = mode,
    questionIds = questionIds,
    currentQuestionIndex = currentQuestionIndex,
    startedAt = startedAt,
    completedAt = completedAt,
    isFinalized = isFinalized
)
