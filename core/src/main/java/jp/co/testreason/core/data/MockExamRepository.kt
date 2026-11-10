package jp.co.testreason.core.data

import androidx.room.withTransaction
import jp.co.testreason.core.database.ExamAnswerEntity
import jp.co.testreason.core.database.MockExamEntity
import jp.co.testreason.core.database.SessionEntity
import jp.co.testreason.core.database.TestReasonDatabase
import jp.co.testreason.core.domain.ExamComposer
import jp.co.testreason.core.domain.ExamComposition
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ExamBlueprint
import jp.co.testreason.core.model.KLevel
import jp.co.testreason.core.model.MockExam
import jp.co.testreason.core.model.MockExamQuestion
import jp.co.testreason.core.model.MockExamResult
import jp.co.testreason.core.model.MockExamResultItem
import jp.co.testreason.core.model.MockExamStart
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.model.ScoreLine
import jp.co.testreason.core.model.StudyMode
import jp.co.testreason.core.model.Syllabus
import jp.co.testreason.core.model.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

interface MockExamRepository {
    /**
     * Composes a 40-question exam from the bank per [ExamBlueprint] and starts the clock.
     * Only one exam can be open: while an exam is unsubmitted, that exam is returned instead of a new one.
     */
    suspend fun startMockExam(extendedTime: Boolean): MockExamStart

    /** The unsubmitted exam to resume, if any. */
    fun observeUnsubmittedExamId(): Flow<String?>

    fun observeExam(sessionId: String): Flow<MockExam?>

    /** Replaces the answer; ignored after submission or after the deadline. Returns whether it was saved. */
    suspend fun answer(sessionId: String, questionId: String, choiceIds: Set<String>): Boolean

    /**
     * Applies one tap on a choice to the saved answer: a single-answer question takes the tapped choice; a
     * multi-answer question toggles it, up to the required number. The saved answer is read and written in one
     * transaction, so quick taps cannot overwrite each other. Same conditions and result as [answer].
     */
    suspend fun selectChoice(sessionId: String, questionId: String, choiceId: String): Boolean

    suspend fun toggleFlag(sessionId: String, questionId: String)

    /**
     * Grades and closes the exam (idempotent). Answered questions are recorded as attempts, so they update
     * mastery and review schedules like any study answer; unanswered questions only count as wrong in the score.
     */
    suspend fun submit(sessionId: String): MockExamResult?

    suspend fun getResult(sessionId: String): MockExamResult?
}

@Singleton
class MockExamRepositoryImpl @Inject constructor(
    private val db: TestReasonDatabase,
    private val quizRepository: QuizRepository,
    private val masteryRepository: MasteryRepository,
    private val spacedReviewEngine: SpacedReviewEngine,
    private val timeProvider: TimeProvider
) : MockExamRepository {

    internal var random: Random = Random.Default

    private val examDao get() = db.mockExamDao()
    private val sessionDao get() = db.sessionDao()
    private val questionDao get() = db.questionDao()

    override suspend fun startMockExam(extendedTime: Boolean): MockExamStart {
        quizRepository.syncBundledQuestions()
        val bank = quizRepository.getAllQuestions().first()
        return when (val composition = ExamComposer.compose(bank, random)) {
            is ExamComposition.Incomplete -> MockExamStart.NotEnoughQuestions(composition.missingGroups)
            is ExamComposition.Ready -> {
                val now = timeProvider.currentTimeMillis()
                val newId = "session_mock_exam_$now"
                // Check and insert in one transaction, so two quick taps cannot open two exams.
                val sessionId = db.withTransaction {
                    examDao.getUnsubmittedExam()?.sessionId ?: newId.also {
                        sessionDao.insertOrUpdateSession(
                            SessionEntity(newId, StudyMode.MOCK_EXAM, composition.questions.map { it.id }, 0, now, null, false)
                        )
                        examDao.insertExam(MockExamEntity(newId, ExamBlueprint.durationMinutes(extendedTime), null))
                    }
                }
                MockExamStart.Started(sessionId)
            }
        }
    }

    override fun observeUnsubmittedExamId(): Flow<String?> = examDao.observeUnsubmittedExam().map { it?.sessionId }

    override fun observeExam(sessionId: String): Flow<MockExam?> =
        combine(examDao.observeExam(sessionId), examDao.observeAnswers(sessionId)) { exam, answers -> exam to answers }
            .map { (exam, answers) -> exam?.let { load(it, answers) } }

    override suspend fun answer(sessionId: String, questionId: String, choiceIds: Set<String>): Boolean =
        updateAnswer(sessionId, questionId) { _, _ -> choiceIds }

    override suspend fun selectChoice(sessionId: String, questionId: String, choiceId: String): Boolean =
        updateAnswer(sessionId, questionId) { current, required ->
            when {
                required == 1 -> setOf(choiceId)
                choiceId in current -> current - choiceId
                current.size < required -> current + choiceId
                else -> current
            }
        }

    /** Reads the saved answer, applies [change] (current selection, required count) and saves, in one transaction. */
    private suspend fun updateAnswer(
        sessionId: String,
        questionId: String,
        change: (current: Set<String>, required: Int) -> Set<String>
    ): Boolean = db.withTransaction {
        val exam = examDao.getExam(sessionId) ?: return@withTransaction false
        val session = sessionDao.getSessionById(sessionId) ?: return@withTransaction false
        val deadline = session.startedAt + exam.durationMinutes * MINUTE_MS
        if (exam.submittedAt != null || timeProvider.currentTimeMillis() > deadline) return@withTransaction false
        if (questionId !in session.questionIds) return@withTransaction false
        val question = questionDao.getQuestionById(questionId) ?: return@withTransaction false
        val saved = examDao.getAnswer(sessionId, questionId)
        val choiceIds = change(saved?.selectedChoiceIds.toChoiceIds(), question.selectCount)
        examDao.upsertAnswer(ExamAnswerEntity(sessionId, questionId, choiceIds.sorted().joinToString(SEPARATOR), saved?.flagged ?: false))
        true
    }

    private fun String?.toChoiceIds(): Set<String> = orEmpty().split(SEPARATOR).filter { it.isNotEmpty() }.toSet()

    override suspend fun toggleFlag(sessionId: String, questionId: String) {
        db.withTransaction {
            val exam = examDao.getExam(sessionId) ?: return@withTransaction
            if (exam.submittedAt != null) return@withTransaction
            val current = examDao.getAnswer(sessionId, questionId)
            examDao.upsertAnswer(
                ExamAnswerEntity(sessionId, questionId, current?.selectedChoiceIds.orEmpty(), !(current?.flagged ?: false))
            )
        }
    }

    override suspend fun submit(sessionId: String): MockExamResult? {
        db.withTransaction {
            val now = timeProvider.currentTimeMillis()
            if (examDao.markSubmitted(sessionId, now) == 0) return@withTransaction // already submitted or unknown
            val exam = load(checkNotNull(examDao.getExam(sessionId)), examDao.getAnswers(sessionId)) ?: return@withTransaction
            exam.questions.filter { it.isAnswered }.forEach { item -> recordAttempt(sessionId, item, now) }
            sessionDao.getSessionById(sessionId)?.let { session ->
                sessionDao.insertOrUpdateSession(
                    session.copy(currentQuestionIndex = session.questionIds.size, isFinalized = true, completedAt = now)
                )
            }
        }
        return getResult(sessionId)
    }

    private suspend fun recordAttempt(sessionId: String, item: MockExamQuestion, now: Long) {
        val attempt = Attempt(
            id = "exam_${sessionId}_${item.question.id}",
            sessionId = sessionId,
            questionId = item.question.id,
            selectedChoiceIds = item.selectedChoiceIds.sorted(),
            isCorrect = item.question.isCorrectSelection(item.selectedChoiceIds),
            confidence = null,
            mistakeReason = null,
            timeSpentMs = 0L,
            timestamp = now
        )
        if (db.attemptDao().insertAttemptIgnore(attempt.toEntity()) > 0) {
            val previous = db.reviewScheduleDao().getSchedule(attempt.questionId)?.toDomain()
            db.reviewScheduleDao().insertOrUpdateSchedule(spacedReviewEngine.calculateSchedule(attempt, previous).toEntity())
            masteryRepository.updateMastery(item.question.learningObjectiveId, attempt.isCorrect, attempt.confidence)
        }
    }

    override suspend fun getResult(sessionId: String): MockExamResult? {
        val entity = examDao.getExam(sessionId) ?: return null
        if (entity.submittedAt == null) return null
        val exam = load(entity, examDao.getAnswers(sessionId)) ?: return null
        val items = exam.questions.mapIndexed { index, item ->
            MockExamResultItem(index + 1, item.question, item.selectedChoiceIds, item.isAnswered && item.question.isCorrectSelection(item.selectedChoiceIds))
        }
        return MockExamResult(
            sessionId = sessionId,
            correct = items.count { it.isCorrect },
            total = items.size,
            byChapter = Syllabus.chapters.map { chapter ->
                val inChapter = items.filter { it.question.learningObjective?.chapter == chapter.number }
                ScoreLine("第${chapter.number}章 ${chapter.title}", inChapter.count { it.isCorrect }, inChapter.size)
            },
            byKLevel = KLevel.entries.map { level ->
                val inLevel = items.filter { it.question.learningObjective?.kLevel == level }
                ScoreLine(level.name, inLevel.count { it.isCorrect }, inLevel.size)
            },
            items = items
        )
    }

    private suspend fun load(entity: MockExamEntity, answers: List<ExamAnswerEntity>): MockExam? {
        val session = sessionDao.getSessionById(entity.sessionId) ?: return null
        val answerByQuestion = answers.associateBy { it.questionId }
        val questions = questionDao.getQuestionsInOrder(session.questionIds).map { it.toDomain() }
        return MockExam(
            sessionId = entity.sessionId,
            questions = questions.map { question: Question ->
                val answer = answerByQuestion[question.id]
                MockExamQuestion(
                    question = question,
                    selectedChoiceIds = answer?.selectedChoiceIds.toChoiceIds(),
                    flagged = answer?.flagged ?: false
                )
            },
            startedAt = session.startedAt,
            durationMinutes = entity.durationMinutes,
            submittedAt = entity.submittedAt
        )
    }

    private companion object {
        const val SEPARATOR = ","
        const val MINUTE_MS = 60_000L
    }
}
