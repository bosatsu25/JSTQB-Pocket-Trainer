package jp.co.testreason.core.data

import androidx.room.withTransaction
import jp.co.testreason.core.content.BundledQuestion
import jp.co.testreason.core.content.BundledQuestionSource
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface QuizRepository {
    fun getAllQuestions(): Flow<List<Question>>
    suspend fun getQuestionById(id: String): Question?
    /**
     * Upserts bundled questions whose revision changed and retires questions that are no longer bundled.
     * Safe to call repeatedly; the work runs once per process.
     */
    suspend fun syncBundledQuestions()
    /**
     * Today's mix: due reviews first, then weak questions, then never-answered questions,
     * then random ones. Returns null when there is nothing to ask.
     */
    suspend fun createDailySession(count: Int = 5): QuizSession?

    /** Returns null when no question is due, instead of creating an empty session. */
    suspend fun createDueReviewSession(): QuizSession?

    /** Returns null when there is no weak question, instead of creating an empty session. */
    suspend fun createWeaknessSession(): QuizSession?
    /**
     * Questions of one syllabus chapter: never answered first, then weak ones (oldest first), then the rest
     * (least recently answered first). [count] null means the whole chapter. Returns null for an empty chapter.
     */
    suspend fun createChapterSession(chapter: Int, count: Int?): QuizSession?

    /** Up to [count] questions of one learning objective, in the same priority order as chapter sessions. */
    suspend fun createLearningObjectiveSession(learningObjectiveId: String, count: Int): QuizSession?

    suspend fun getSessionById(id: String): QuizSession?
    fun getActiveSession(): Flow<QuizSession?>
    
    suspend fun finalizeAttempt(attempt: Attempt): Boolean
    /** Tags a wrong answer and re-schedules its review, since the reason decides the interval. */
    suspend fun updateMistakeReason(sessionId: String, questionId: String, mistakeReason: MistakeReason)
    
    fun getAttemptsForSession(sessionId: String): Flow<List<Attempt>>

    /** Every answer ever given (for progress analytics). */
    fun observeAllAttempts(): Flow<List<Attempt>>
    /** Questions whose review is due now; re-evaluated as the clock moves, not only when data changes. */
    fun observeDueReviewQuestions(): Flow<List<Question>>

    /** Questions whose latest answer was wrong, or right but guessed / low confidence. */
    fun observeWeakQuestions(): Flow<List<Question>>
    
    suspend fun completeQuestion(sessionId: String, questionId: String): QuizSession?
}

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val db: TestReasonDatabase,
    private val masteryRepository: MasteryRepository,
    private val spacedReviewEngine: SpacedReviewEngine,
    private val timeProvider: TimeProvider,
    private val bundledQuestionSource: BundledQuestionSource
) : QuizRepository {

    private val syncMutex = Mutex()

    private companion object {
        const val WEAKNESS_SESSION_SIZE = 5
        const val CLOCK_TICK_MS = 60_000L
    }

    @Volatile
    private var isSynced = false

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

    override suspend fun syncBundledQuestions() {
        syncMutex.withLock {
            if (isSynced) return
            val bundled = withContext(Dispatchers.IO) { bundledQuestionSource.load() }
            // An empty bundle means the assets could not be read; never retire the whole bank because of it.
            if (bundled.isNotEmpty()) {
                applyBundle(bundled)
            }
            isSynced = true
        }
    }

    private suspend fun applyBundle(bundled: List<BundledQuestion>) {
        db.withTransaction {
            val existing = questionDao.getQuestionRevisions().associateBy { it.id }
            val activeBundle = bundled.filter { it.active }.map { it.question }
            val activeIds = activeBundle.map { it.id }.toSet()

            val changed = activeBundle.filter { question ->
                val current = existing[question.id]
                current == null || current.revision != question.revision || !current.isActive
            }
            if (changed.isNotEmpty()) {
                questionDao.insertQuestions(changed.map { it.toEntity() })
            }

            val retired = existing.values.filter { it.isActive && it.id !in activeIds }.map { it.id }
            if (retired.isNotEmpty()) {
                questionDao.deactivateQuestions(retired)
            }
        }
    }

    override suspend fun createDailySession(count: Int): QuizSession? {
        syncBundledQuestions()
        val picked = LinkedHashSet<String>()
        fun addAll(ids: List<String>) = ids.forEach { if (picked.size < count) picked += it }

        addAll(observeDueReviewQuestions().first().map { it.id })
        addAll(observeWeakQuestions().first().map { it.id })
        addAll(questionDao.getUnseenQuestions(count).map { it.id })
        addAll(questionDao.getRandomQuestions(count * 2).map { it.id })
        return createSession(StudyMode.DAILY, picked.toList())
    }

    override suspend fun createDueReviewSession(): QuizSession? {
        syncBundledQuestions()
        return createSession(StudyMode.REVIEW, observeDueReviewQuestions().first().map { it.id })
    }

    override suspend fun createWeaknessSession(): QuizSession? {
        syncBundledQuestions()
        return createSession(StudyMode.WEAKNESS, observeWeakQuestions().first().take(WEAKNESS_SESSION_SIZE).map { it.id })
    }

    override suspend fun createChapterSession(chapter: Int, count: Int?): QuizSession? {
        syncBundledQuestions()
        val questions = questionDao.getActiveQuestionsByLoPrefix("FL-$chapter.").map { it.id }
        return createSession(StudyMode.CHAPTER, prioritise(questions).let { if (count != null) it.take(count) else it })
    }

    override suspend fun createLearningObjectiveSession(learningObjectiveId: String, count: Int): QuizSession? {
        syncBundledQuestions()
        val questions = questionDao.getActiveQuestionsByLo(learningObjectiveId).map { it.id }
        return createSession(StudyMode.CUSTOM, prioritise(questions).take(count))
    }

    /** Unseen (shuffled) -> weak (oldest answer first) -> others (oldest answer first). */
    private suspend fun prioritise(questionIds: List<String>): List<String> {
        val latest = attemptDao.getLatestAttemptPerQuestion().first()
            .groupBy { it.questionId }
            .mapValues { (_, attempts) -> attempts.maxBy { it.timestamp } }
        val (seen, unseen) = questionIds.partition { it in latest }
        val (weak, others) = seen.sortedBy { latest.getValue(it).timestamp }.partition { id ->
            val attempt = latest.getValue(id)
            !attempt.isCorrect || attempt.confidence == ConfidenceLevel.GUESS || attempt.confidence == ConfidenceLevel.LOW
        }
        return unseen.shuffled() + weak + others
    }

    private suspend fun createSession(mode: StudyMode, questionIds: List<String>): QuizSession? {
        if (questionIds.isEmpty()) return null
        val now = timeProvider.currentTimeMillis()
        val session = QuizSession(
            id = "session_${mode.name.lowercase()}_$now",
            mode = mode,
            questionIds = questionIds,
            currentQuestionIndex = 0,
            startedAt = now,
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
                val previous = reviewScheduleDao.getSchedule(attempt.questionId)?.toDomain()
                val schedule = spacedReviewEngine.calculateSchedule(attempt, previous)
                reviewScheduleDao.insertOrUpdateSchedule(schedule.toEntity())
                true
            } else {
                false
            }
        }
    }

    override suspend fun updateMistakeReason(sessionId: String, questionId: String, mistakeReason: MistakeReason) {
        db.withTransaction {
            attemptDao.updateMistakeReasonOnly(sessionId, questionId, mistakeReason)
            val attempt = attemptDao.getAttempt(sessionId, questionId)?.toDomain() ?: return@withTransaction
            if (attempt.isCorrect) return@withTransaction
            // Only re-schedule while this attempt is still the one the schedule was built from.
            val current = reviewScheduleDao.getSchedule(questionId)
            if (current == null || current.lastAttemptAt == attempt.timestamp) {
                // A miss resets the interval, so the previous schedule does not matter here.
                reviewScheduleDao.insertOrUpdateSchedule(spacedReviewEngine.calculateSchedule(attempt).toEntity())
            }
        }
    }

    override fun getAttemptsForSession(sessionId: String): Flow<List<Attempt>> {
        return attemptDao.getAttemptsBySession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeAllAttempts(): Flow<List<Attempt>> =
        attemptDao.getAllAttempts().map { entities -> entities.map { it.toDomain() } }

    override fun observeDueReviewQuestions(): Flow<List<Question>> =
        combine(reviewScheduleDao.getAllSchedulesOrdered(), clockTicks()) { schedules, _ ->
            val now = timeProvider.currentTimeMillis()
            schedules.filter { it.nextReviewAt <= now }.map { it.questionId }
        }.distinctUntilChanged().map { ids -> activeQuestions(ids) }

    override fun observeWeakQuestions(): Flow<List<Question>> =
        attemptDao.getLatestAttemptPerQuestion().map { latest ->
            val weakIds = latest.distinctBy { it.questionId }
                .sortedByDescending { it.timestamp }
                .filter { attempt ->
                    !attempt.isCorrect ||
                        attempt.confidence == ConfidenceLevel.GUESS ||
                        attempt.confidence == ConfidenceLevel.LOW
                }
                .map { it.questionId }
            activeQuestions(weakIds)
        }

    private suspend fun activeQuestions(ids: List<String>): List<Question> =
        ids.mapNotNull { id -> questionDao.getQuestionById(id)?.takeIf { it.isActive }?.toDomain() }

    /**
     * Emits immediately and then periodically so time-based queries notice that time has passed.
     * Runs on [Dispatchers.Default] so the endless loop never occupies (or, in tests, the virtual clock of)
     * the collector's dispatcher.
     */
    private fun clockTicks(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(CLOCK_TICK_MS)
        }
    }.flowOn(Dispatchers.Default)

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
                    masteryRepository.updateMastery(question.learningObjectiveId, attempt.isCorrect, attempt.confidence)
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
    explanation = explanation,
    selectCount = selectCount,
    scenario = scenario,
    revision = revision
)

fun Question.toEntity(isActive: Boolean = true) = QuestionEntity(
    id = id,
    learningObjectiveId = learningObjectiveId,
    stem = stem,
    choices = choices.map { ChoiceEntity(it.id, it.text, it.isCorrect, it.distractorAnalysis) },
    explanation = explanation,
    selectCount = selectCount,
    scenario = scenario,
    revision = revision,
    isActive = isActive
)

private const val CHOICE_ID_SEPARATOR = ","

fun AttemptEntity.toDomain() = Attempt(
    id = id,
    sessionId = sessionId,
    questionId = questionId,
    selectedChoiceIds = selectedChoiceId.split(CHOICE_ID_SEPARATOR).filter { it.isNotEmpty() },
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
    selectedChoiceId = selectedChoiceIds.sorted().joinToString(CHOICE_ID_SEPARATOR),
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
