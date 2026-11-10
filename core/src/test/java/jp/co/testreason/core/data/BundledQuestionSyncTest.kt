package jp.co.testreason.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.content.BundledQuestion
import jp.co.testreason.core.content.BundledQuestionSource
import jp.co.testreason.core.database.TestReasonDatabase
import jp.co.testreason.core.domain.SpacedReviewEngine
import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.Question
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BundledQuestionSyncTest {

    private lateinit var db: TestReasonDatabase
    private val time = MutableTimeProvider(1_000L)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TestReasonDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** A new repository instance simulates a new app process (sync runs once per process). */
    private suspend fun syncWith(vararg questions: BundledQuestion) {
        QuizRepositoryImpl(
            db = db,
            masteryRepository = FakeMasteryRepository(),
            spacedReviewEngine = SpacedReviewEngine(time),
            timeProvider = time,
            bundledQuestionSource = BundledQuestionSource { questions.toList() }
        ).syncBundledQuestions()
    }

    private fun question(id: String, revision: Int, stem: String = "stem $id r$revision") = Question(
        id = id,
        learningObjectiveId = "FL-1.1.1",
        stem = stem,
        choices = listOf(
            Choice("A", "a", true, "ok"),
            Choice("B", "b", false, "ng"),
            Choice("C", "c", false, "ng"),
            Choice("D", "d", false, "ng")
        ),
        explanation = "exp",
        revision = revision
    )

    @Test
    fun appUpdate_addsNewQuestionsToExistingDatabase() = runTest {
        syncWith(BundledQuestion(question("c1-001", 1), true))
        syncWith(BundledQuestion(question("c1-001", 1), true), BundledQuestion(question("c1-002", 1), true))

        assertEquals(setOf("c1-001", "c1-002"), db.questionDao().getAllQuestions().first().map { it.id }.toSet())
    }

    @Test
    fun revisionBump_updatesQuestionText() = runTest {
        syncWith(BundledQuestion(question("c1-001", 1, stem = "old"), true))
        syncWith(BundledQuestion(question("c1-001", 2, stem = "fixed"), true))

        val stored = db.questionDao().getQuestionById("c1-001")!!
        assertEquals("fixed", stored.stem)
        assertEquals(2, stored.revision)
    }

    @Test
    fun removedOrRetiredQuestions_areDeactivatedButKept() = runTest {
        syncWith(
            BundledQuestion(question("c1-001", 1), true),
            BundledQuestion(question("c1-002", 1), true),
            BundledQuestion(question("c1-003", 1), true)
        )
        syncWith(
            BundledQuestion(question("c1-001", 1), true),
            BundledQuestion(question("c1-002", 2), false)
        )

        assertEquals(listOf("c1-001"), db.questionDao().getAllQuestions().first().map { it.id })
        assertFalse(db.questionDao().getQuestionById("c1-002")!!.isActive)
        assertFalse(db.questionDao().getQuestionById("c1-003")!!.isActive)
        assertEquals(1, db.questionDao().getRandomQuestions(10).size)
    }

    @Test
    fun emptyBundle_neverRetiresTheQuestionBank() = runTest {
        syncWith(BundledQuestion(question("c1-001", 1), true))
        syncWith()

        assertTrue(db.questionDao().getQuestionById("c1-001")!!.isActive)
    }
}
