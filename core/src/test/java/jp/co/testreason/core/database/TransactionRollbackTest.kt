package jp.co.testreason.core.database

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.StudyMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransactionRollbackTest {

    private lateinit var db: TestReasonDatabase

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

    @Test
    fun transactionRollback_revertsAttemptAndCompletionOnException() = runTest {
        val session = SessionEntity(
            id = "s_trans_1",
            mode = StudyMode.DAILY,
            questionIds = listOf("q1"),
            currentQuestionIndex = 0,
            startedAt = 1000L,
            completedAt = null,
            isFinalized = false
        )
        db.sessionDao().insertOrUpdateSession(session)

        try {
            db.withTransaction {
                db.attemptDao().insertAttemptIgnore(
                    AttemptEntity(
                        id = "att_t1",
                        sessionId = "s_trans_1",
                        questionId = "q1",
                        selectedChoiceId = "A",
                        isCorrect = false,
                        confidence = ConfidenceLevel.LOW,
                        mistakeReason = null,
                        timeSpentMs = 1000,
                        timestamp = 1000L
                    )
                )

                db.questionCompletionDao().insertCompletionIgnore(
                    QuestionCompletionEntity(
                        sessionId = "s_trans_1",
                        questionId = "q1",
                        completedAt = 1005L
                    )
                )

                // Intentional error to trigger transaction rollback
                throw IllegalStateException("Intentional transaction error for testing rollback")
            }
            fail("Expected exception was not thrown!")
        } catch (e: IllegalStateException) {
            // Expected
        }

        // Assert that nothing was committed due to transaction rollback
        val attempt = db.attemptDao().getAttempt("s_trans_1", "q1")
        assertNull("Attempt should be null after transaction rollback", attempt)

        val completion = db.questionCompletionDao().getCompletion("s_trans_1", "q1")
        assertNull("Completion should be null after transaction rollback", completion)
    }
}
