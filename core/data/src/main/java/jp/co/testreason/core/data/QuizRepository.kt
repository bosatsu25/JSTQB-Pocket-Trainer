package jp.co.testreason.core.data

import androidx.room.withTransaction
import jp.co.testreason.core.database.AttemptEntity
import jp.co.testreason.core.database.ChoiceEntity
import jp.co.testreason.core.database.QuestionCompletionEntity
import jp.co.testreason.core.database.QuestionEntity
import jp.co.testreason.core.database.ReviewScheduleEntity
import jp.co.testreason.core.database.SessionEntity
import jp.co.testreason.core.database.TestReasonDatabase
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.model.QuizSession
import jp.co.testreason.core.model.ReviewSchedule
import jp.co.testreason.core.model.StudyMode
import jp.co.testreason.core.model.TimeProvider
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
    suspend fun createDueReviewSession(): QuizSession
    suspend fun createWeaknessSession(): QuizSession
    suspend fun getSessionById(id: String): QuizSession?
    fun getActiveSession(): Flow<QuizSession?>
    
    suspend fun finalizeAttempt(attempt: Attempt): Boolean
    suspend fun updateMistakeReason(sessionId: String, questionId: String, mistakeReason: MistakeReason)
    
    fun getAttemptsForSession(sessionId: String): Flow<List<Attempt>>
    fun observeDueReviewQuestions(): Flow<List<Question>>
    fun observeWeakQuestions(): Flow<List<Question>>
    
    suspend fun completeQuestion(sessionId: String, questionId: String): QuizSession?
}

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val db: TestReasonDatabase,
    private val masteryRepository: MasteryRepository,
    private val spacedReviewEngine: SpacedReviewEngine,
    private val timeProvider: TimeProvider
) : QuizRepository {

    private val questionDao get() = db.questionDao()
    private val attemptDao get() = db.attemptDao()
    private val sessionDao get() = db.sessionDao()
    private val reviewScheduleDao get() = db.reviewScheduleDao()
    private val questionCompletionDao get() = db.questionCompletionDao()

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
            id = "session_" + timeProvider.currentTimeMillis(),
            mode = StudyMode.DAILY,
            questionIds = questions.map { it.id },
            currentQuestionIndex = 0,
            startedAt = timeProvider.currentTimeMillis(),
            completedAt = null,
            isFinalized = false
        )
        sessionDao.insertOrUpdateSession(session.toEntity())
        return session
    }

    override suspend fun createDueReviewSession(): QuizSession {
        seedInitialQuestionsIfEmpty()
        val dueQuestions = observeDueReviewQuestions().first()
        val session = QuizSession(
            id = "session_review_" + timeProvider.currentTimeMillis(),
            mode = StudyMode.REVIEW,
            questionIds = dueQuestions.map { it.id },
            currentQuestionIndex = 0,
            startedAt = timeProvider.currentTimeMillis(),
            completedAt = null,
            isFinalized = false
        )
        sessionDao.insertOrUpdateSession(session.toEntity())
        return session
    }

    override suspend fun createWeaknessSession(): QuizSession {
        seedInitialQuestionsIfEmpty()
        val weakQuestions = observeWeakQuestions().first()
        val sessionQuestions = if (weakQuestions.isNotEmpty()) {
            weakQuestions.take(5)
        } else {
            emptyList()
        }
        val session = QuizSession(
            id = "session_weakness_" + timeProvider.currentTimeMillis(),
            mode = StudyMode.WEAKNESS,
            questionIds = sessionQuestions.map { it.id },
            currentQuestionIndex = 0,
            startedAt = timeProvider.currentTimeMillis(),
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

    override suspend fun finalizeAttempt(attempt: Attempt): Boolean {
        return db.withTransaction {
            val session = sessionDao.getSessionById(attempt.sessionId) ?: return@withTransaction false
            if (session.isFinalized) return@withTransaction false

            val expectedQuestionId = session.questionIds.getOrNull(session.currentQuestionIndex)
            if (expectedQuestionId != attempt.questionId) return@withTransaction false

            val insertedRowId = attemptDao.insertAttemptIgnore(attempt.toEntity())
            if (insertedRowId > 0) {
                val schedule = spacedReviewEngine.calculateSchedule(attempt)
                reviewScheduleDao.insertOrUpdateSchedule(schedule.toEntity())
                true
            } else {
                false
            }
        }
    }

    override suspend fun updateMistakeReason(sessionId: String, questionId: String, mistakeReason: MistakeReason) {
        attemptDao.updateMistakeReasonOnly(sessionId, questionId, mistakeReason)
    }

    override fun getAttemptsForSession(sessionId: String): Flow<List<Attempt>> {
        return attemptDao.getAttemptsBySession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeDueReviewQuestions(): Flow<List<Question>> {
        val now = timeProvider.currentTimeMillis()
        return reviewScheduleDao.getDueReviewSchedules(now).map { schedules ->
            schedules.mapNotNull { sched ->
                questionDao.getQuestionById(sched.questionId)?.toDomain()
            }
        }
    }

    override fun observeWeakQuestions(): Flow<List<Question>> {
        return attemptDao.getAllAttempts().map { attempts ->
            val weakQuestionIds = attempts.filter { attempt ->
                !attempt.isCorrect ||
                        attempt.confidence == ConfidenceLevel.GUESS ||
                        attempt.confidence == ConfidenceLevel.LOW
            }.map { it.questionId }.distinct()

            weakQuestionIds.mapNotNull { qId ->
                questionDao.getQuestionById(qId)?.toDomain()
            }
        }
    }

    override suspend fun completeQuestion(sessionId: String, questionId: String): QuizSession? {
        return db.withTransaction {
            val session = sessionDao.getSessionById(sessionId) ?: return@withTransaction null
            if (session.isFinalized) return@withTransaction session.toDomain()

            val expectedQuestionId = session.questionIds.getOrNull(session.currentQuestionIndex)
            if (expectedQuestionId != questionId) return@withTransaction session.toDomain()

            val attempt = attemptDao.getAttempt(sessionId, questionId) ?: return@withTransaction session.toDomain()

            val completion = QuestionCompletionEntity(
                sessionId = sessionId,
                questionId = questionId,
                completedAt = timeProvider.currentTimeMillis()
            )
            val isFirstCompletion = questionCompletionDao.insertCompletionIgnore(completion) > 0

            if (isFirstCompletion) {
                val question = questionDao.getQuestionById(questionId)
                if (question != null) {
                    masteryRepository.updateMastery(question.learningObjectiveId, attempt.isCorrect)
                }

                val nextIndex = session.currentQuestionIndex + 1
                val isFinalized = nextIndex >= session.questionIds.size

                val updated = session.copy(
                    currentQuestionIndex = nextIndex,
                    isFinalized = isFinalized,
                    completedAt = if (isFinalized) timeProvider.currentTimeMillis() else null
                )
                sessionDao.insertOrUpdateSession(updated)
                updated.toDomain()
            } else {
                session.toDomain()
            }
        }
    }
}

// Mappers
fun ReviewScheduleEntity.toDomain() = ReviewSchedule(
    questionId = questionId,
    nextReviewAt = nextReviewAt,
    lastAttemptAt = lastAttemptAt,
    intervalDays = intervalDays
)

fun ReviewSchedule.toEntity() = ReviewScheduleEntity(
    questionId = questionId,
    nextReviewAt = nextReviewAt,
    lastAttemptAt = lastAttemptAt,
    intervalDays = intervalDays
)

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
