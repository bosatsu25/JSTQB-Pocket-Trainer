package jp.co.testreason.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.content.BundledQuestion
import jp.co.testreason.core.content.BundledQuestionSource
import jp.co.testreason.core.database.TestReasonDatabase
import jp.co.testreason.core.domain.ExamTestBank
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.MockExamStart
import jp.co.testreason.core.model.StudyMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class MockExamRepositoryTest {

    private lateinit var db: TestReasonDatabase
    private lateinit var quiz: QuizRepository
    private lateinit var exams: MockExamRepositoryImpl
    private val time = MutableTimeProvider(1_000_000L)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TestReasonDatabase::class.java)
            .allowMainThreadQueries().build()
        val bank = ExamTestBank.questions(perLearningObjective = 2)
        val engine = SpacedReviewEngine(time)
        val mastery = MasteryRepositoryImpl(db.masteryDao(), time)
        quiz = QuizRepositoryImpl(db, mastery, engine, time, BundledQuestionSource { bank.map { BundledQuestion(it, true) } })
        exams = MockExamRepositoryImpl(db, quiz, mastery, engine, time).also { it.random = Random(3) }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun start(extended: Boolean = false) = (exams.startMockExam(extended) as MockExamStart.Started).sessionId

    @Test
    fun start_createsA40QuestionTimedExam_thatIsNotAStudySessionToResume() = runTest {
        val id = start()
        val exam = exams.observeExam(id).first()!!

        assertEquals(40, exam.questions.size)
        assertEquals(60, exam.durationMinutes)
        assertEquals(1_000_000L + 60 * 60_000L, exam.deadline)
        assertEquals(id, exams.observeUnsubmittedExamId().first())
        assertNull(quiz.getActiveSession().first())
        assertEquals(StudyMode.MOCK_EXAM, quiz.getSessionById(id)!!.mode)
        time.timeMs += 1
        assertEquals(75, exams.observeExam(start(extended = true)).first()!!.durationMinutes)
    }

    @Test
    fun answers_canChangeUntilSubmit_andFlagsAreKept() = runTest {
        val id = start()
        val q = exams.observeExam(id).first()!!.questions[0].question.id

        assertTrue(exams.answer(id, q, setOf("B")))
        exams.toggleFlag(id, q)
        assertTrue(exams.answer(id, q, setOf("A")))

        val item = exams.observeExam(id).first()!!.questions[0]
        assertEquals(setOf("A"), item.selectedChoiceIds)
        assertTrue(item.flagged)
    }

    @Test
    fun answersAfterTheDeadline_areRejected() = runTest {
        val id = start()
        val q = exams.observeExam(id).first()!!.questions[0].question.id
        time.timeMs += 60 * 60_000L + 1

        assertFalse(exams.answer(id, q, setOf("A")))
    }

    @Test
    fun submit_gradesWithThe26PointPassLine_andRecordsOnlyAnsweredQuestions() = runTest {
        val id = start()
        val questions = exams.observeExam(id).first()!!.questions.map { it.question.id }
        // 25 correct, 5 wrong, 10 unanswered -> fail.
        questions.take(25).forEach { exams.answer(id, it, setOf("A")) }
        questions.drop(25).take(5).forEach { exams.answer(id, it, setOf("B")) }

        val result = exams.submit(id)!!
        assertEquals(25, result.correct)
        assertFalse(result.passed)
        assertEquals(10, result.unanswered)
        assertEquals(40, result.byChapter.sumOf { it.total })
        assertEquals(listOf(8, 24, 8), result.byKLevel.map { it.total })
        assertEquals(30, db.attemptDao().getAttemptsBySession(id).first().size)
        assertTrue(quiz.getSessionById(id)!!.isFinalized)
        assertNull(exams.observeUnsubmittedExamId().first())

        // Idempotent, and closed for edits.
        assertEquals(25, exams.submit(id)!!.correct)
        assertFalse(exams.answer(id, questions[39], setOf("A")))
    }

    @Test
    fun twentySixCorrect_passes() = runTest {
        val id = start()
        exams.observeExam(id).first()!!.questions.take(26).forEach { exams.answer(id, it.question.id, setOf("A")) }
        assertTrue(exams.submit(id)!!.passed)
    }

    @Test
    fun smallBank_reportsMissingGroups() = runTest {
        val small = QuizRepositoryImpl(
            db, MasteryRepositoryImpl(db.masteryDao(), time), SpacedReviewEngine(time), time,
            BundledQuestionSource { ExamTestBank.questions(1).filter { it.learningObjectiveId.startsWith("FL-1.") }.map { BundledQuestion(it, true) } }
        )
        val repo = MockExamRepositoryImpl(db, small, MasteryRepositoryImpl(db.masteryDao(), time), SpacedReviewEngine(time), time)
        val start = repo.startMockExam(extendedTime = false)
        assertTrue(start is MockExamStart.NotEnoughQuestions)
    }
}
