package jp.co.testreason.feature.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.content.BundledQuestion
import jp.co.testreason.core.content.BundledQuestionSource
import jp.co.testreason.core.data.MasteryRepository
import jp.co.testreason.core.data.MasteryRepositoryImpl
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
import jp.co.testreason.core.model.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

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
}
