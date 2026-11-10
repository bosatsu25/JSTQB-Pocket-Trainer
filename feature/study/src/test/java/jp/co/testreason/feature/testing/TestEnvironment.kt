package jp.co.testreason.feature.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.content.BundledQuestion
import jp.co.testreason.core.content.BundledQuestionSource
import jp.co.testreason.core.data.MasteryRepository
import jp.co.testreason.core.data.MasteryRepositoryImpl
import jp.co.testreason.core.data.MockExamRepository
import jp.co.testreason.core.data.MockExamRepositoryImpl
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.data.QuizRepositoryImpl
import jp.co.testreason.core.database.AttemptEntity
import jp.co.testreason.core.database.SessionEntity
import jp.co.testreason.core.database.TestReasonDatabase
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.model.StudyMode
import jp.co.testreason.core.model.Syllabus
import jp.co.testreason.core.model.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Turbine waits on wall-clock time, while these tests wait on real Room I/O on other threads. Turbine's
 * default of 3 s is too tight on a loaded CI runner (module test JVMs and lint run in parallel).
 */
val REPOSITORY_IO_TIMEOUT: Duration = 10.seconds

/**
 * Runs viewModelScope on a test dispatcher with its own scheduler, so periodic clock ticks in the
 * repository never advance and never block the test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class MutableClock(var now: Long = 1_000_000L) : TimeProvider {
    override fun currentTimeMillis(): Long = now
}

/** Real repositories on an in-memory database with a small, fixed question set. */
class TestEnvironment(questions: List<Question> = SampleData.questions) {
    val clock = MutableClock()
    val db: TestReasonDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        TestReasonDatabase::class.java
    ).allowMainThreadQueries().build()
    val masteryRepository: MasteryRepository = MasteryRepositoryImpl(db.masteryDao(), clock)
    val quizRepository: QuizRepository = QuizRepositoryImpl(
        db = db,
        masteryRepository = masteryRepository,
        spacedReviewEngine = SpacedReviewEngine(clock),
        timeProvider = clock,
        bundledQuestionSource = BundledQuestionSource { questions.map { BundledQuestion(it, active = true) } }
    )

    val mockExamRepository: MockExamRepository = MockExamRepositoryImpl(
        db, quizRepository, masteryRepository, SpacedReviewEngine(clock), clock
    )

    suspend fun start() = quizRepository.syncBundledQuestions()

    /** Inserts a session with a fixed question order (repository sessions shuffle). */
    suspend fun session(id: String, vararg questionIds: String, index: Int = 0): String {
        db.sessionDao().insertOrUpdateSession(
            SessionEntity(id, StudyMode.DAILY, questionIds.toList(), index, clock.now, null, false)
        )
        return id
    }

    suspend fun answer(sessionId: String, questionId: String, correct: Boolean, confidence: ConfidenceLevel? = ConfidenceLevel.HIGH) {
        db.attemptDao().insertAttemptIgnore(
            AttemptEntity("a_${sessionId}_$questionId", sessionId, questionId, "A", correct, confidence, null, 30_000L, clock.now)
        )
    }

    fun close() = db.close()
}

object SampleData {
    private fun choices(vararg correct: String, count: Int = 4) =
        listOf("A", "B", "C", "D", "E").take(count).map { Choice(it, "選択肢$it", it in correct, "解説$it") }

    val single1 = Question("c1-001", "FL-1.1.1", "テストの目的は？", choices("B"), "解説", revision = 1)
    val single2 = Question("c1-002", "FL-1.3.1", "テストの原則は？", choices("A"), "解説", revision = 1)
    val multi = Question("c4-001", "FL-4.2.2", "境界値を2つ選べ", choices("B", "D", count = 5), "解説", selectCount = 2, scenario = "| a | b |", revision = 1)

    val questions = listOf(single1, single2, multi)

    /** Two questions for every LO (correct answer "A"): enough to compose a mock exam. */
    fun examBank(): List<Question> = Syllabus.learningObjectives.flatMap { lo ->
        (1..2).map { n -> Question("${lo.id}-$n", lo.id, "${lo.id} 問題$n", choices("A"), "解説", revision = 1) }
    }
}
