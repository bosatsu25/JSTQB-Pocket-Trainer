package jp.co.testreason.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.StudyMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseTest {

    private lateinit var db: TestReasonDatabase
    private lateinit var questionDao: QuestionDao
    private lateinit var attemptDao: AttemptDao
    private lateinit var sessionDao: SessionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TestReasonDatabase::class.java
        ).allowMainThreadQueries().build()

        questionDao = db.questionDao()
        attemptDao = db.attemptDao()
        sessionDao = db.sessionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAttempt_returnsCorrectAttemptBySessionAndQuestion() = runTest {
        val attempt1 = AttemptEntity(
            id = "att1",
            sessionId = "s1",
            questionId = "q1",
            selectedChoiceId = "A",
            isCorrect = false,
            confidence = ConfidenceLevel.LOW,
            mistakeReason = null,
            timeSpentMs = 1000,
            timestamp = 1000000
        )
        attemptDao.insertAttemptIgnore(attempt1)

        val retrieved = attemptDao.getAttempt("s1", "q1")
        assertNotNull(retrieved)
        assertEquals("A", retrieved?.selectedChoiceId)
        assertEquals(false, retrieved?.isCorrect)
    }

    @Test
    fun sessionDao_savesAndRestoresActiveSession() = runTest {
        val session = SessionEntity(
            id = "sess_123",
            mode = StudyMode.DAILY,
            questionIds = listOf("q1", "q2", "q3"),
            currentQuestionIndex = 1,
            startedAt = 1000,
            completedAt = null,
            isFinalized = false
        )
        sessionDao.insertOrUpdateSession(session)

        val restored = sessionDao.getSessionById("sess_123")
        assertNotNull(restored)
        assertEquals(1, restored?.currentQuestionIndex)
        assertEquals(3, restored?.questionIds?.size)
    }
}
