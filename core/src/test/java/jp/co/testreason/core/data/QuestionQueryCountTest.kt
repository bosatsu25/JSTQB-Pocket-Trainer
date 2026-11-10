package jp.co.testreason.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.content.BundledQuestion
import jp.co.testreason.core.content.BundledQuestionSource
import jp.co.testreason.core.database.TestReasonDatabase
import jp.co.testreason.core.domain.ExamTestBank
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.MockExamStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lists of questions are loaded with one query, not one query per question (issue #24).
 * The mock exam reloads its 40 questions on every tap, so the difference there was 40 queries per tap.
 */
@RunWith(RobolectricTestRunner::class)
class QuestionQueryCountTest {

    private val queries = CopyOnWriteArrayList<String>()
    private lateinit var db: TestReasonDatabase
    private lateinit var quiz: QuizRepositoryImpl
    private lateinit var exams: MockExamRepositoryImpl
    private val time = MutableTimeProvider(1_000_000L)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TestReasonDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCallback({ sql, _ -> queries += sql }, Runnable::run)
            .build()
        val bank = ExamTestBank.questions(perLearningObjective = 2)
        val engine = SpacedReviewEngine(time)
        val mastery = MasteryRepositoryImpl(db.masteryDao(), time)
        quiz = QuizRepositoryImpl(db, mastery, engine, time, BundledQuestionSource { bank.map { BundledQuestion(it, true) } })
        exams = MockExamRepositoryImpl(db, quiz, mastery, engine, time)
    }

    @After
    fun tearDown() = db.close()

    /** SELECTs that read whole question rows while [block] runs. */
    private suspend fun questionReads(block: suspend () -> Unit): Int {
        queries.clear()
        block()
        return queries.count { it.startsWith("SELECT * FROM questions") }
    }

    @Test
    fun mockExam_isLoadedWithOneQuestionQuery_inSessionOrder() = runTest {
        val id = (exams.startMockExam(false) as MockExamStart.Started).sessionId
        val order = quiz.getSessionById(id)!!.questionIds

        var loaded = emptyList<String>()
        val reads = questionReads { loaded = exams.observeExam(id).first()!!.questions.map { it.question.id } }

        assertEquals(order, loaded)
        assertEquals(1, reads)
    }

    @Test
    fun weakQuestions_areLoadedWithOneQuestionQuery_mostRecentFirst() = runTest {
        quiz.syncBundledQuestions()
        val wrong = quiz.getAllQuestions().first().take(12).map { it.id }
        wrong.forEachIndexed { index, questionId ->
            time.timeMs += 1_000
            db.attemptDao().insertAttemptIgnore(
                Attempt("a$index", "s", questionId, listOf("B"), false, null, null, 1_000L, time.timeMs).toEntity()
            )
        }

        var weak = emptyList<String>()
        val reads = questionReads { weak = quiz.observeWeakQuestions().first().map { it.id } }

        assertEquals(wrong.reversed(), weak)
        assertEquals(1, reads)
    }

    @Test
    fun questionsByIds_keepTheRequestedOrder_skipUnknownIds_andSurviveMoreIdsThanSqliteVariables() = runTest {
        quiz.syncBundledQuestions()
        val ids = quiz.getAllQuestions().first().map { it.id }

        val requested = listOf(ids[5], "unknown", ids[2], ids[5])
        assertEquals(listOf(ids[5], ids[2], ids[5]), quiz.getQuestionsByIds(requested).map { it.id })
        assertTrue(quiz.getQuestionsByIds(emptyList()).isEmpty())

        // More ids than one statement may carry on older devices (999): still answered, in order.
        val many = List(2_000) { ids[it % ids.size] }
        assertEquals(many, quiz.getQuestionsByIds(many).map { it.id })
    }
}
