package jp.co.testreason.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Exam-specific data of a MOCK_EXAM session (question order and start time live in quiz_sessions). */
@Entity(tableName = "mock_exams")
data class MockExamEntity(
    @PrimaryKey val sessionId: String,
    val durationMinutes: Int,
    val submittedAt: Long?
)

/**
 * Provisional exam answers: unlike study attempts they may change until the exam is submitted.
 * Final answers are copied to `attempts` on submission.
 */
@Entity(tableName = "exam_answers", primaryKeys = ["sessionId", "questionId"])
data class ExamAnswerEntity(
    val sessionId: String,
    val questionId: String,
    /** Sorted choice ids joined with ","; empty when only flagged. */
    val selectedChoiceIds: String,
    val flagged: Boolean
)

@Dao
interface MockExamDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExam(exam: MockExamEntity)

    @Query("SELECT * FROM mock_exams WHERE sessionId = :sessionId")
    suspend fun getExam(sessionId: String): MockExamEntity?

    @Query("SELECT * FROM mock_exams WHERE sessionId = :sessionId")
    fun observeExam(sessionId: String): Flow<MockExamEntity?>

    @Query(
        "SELECT m.* FROM mock_exams m JOIN quiz_sessions s ON s.id = m.sessionId " +
            "WHERE m.submittedAt IS NULL ORDER BY s.startedAt DESC LIMIT 1"
    )
    fun observeUnsubmittedExam(): Flow<MockExamEntity?>

    @Query("UPDATE mock_exams SET submittedAt = :submittedAt WHERE sessionId = :sessionId AND submittedAt IS NULL")
    suspend fun markSubmitted(sessionId: String, submittedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnswer(answer: ExamAnswerEntity)

    @Query("SELECT * FROM exam_answers WHERE sessionId = :sessionId AND questionId = :questionId")
    suspend fun getAnswer(sessionId: String, questionId: String): ExamAnswerEntity?

    @Query("SELECT * FROM exam_answers WHERE sessionId = :sessionId")
    suspend fun getAnswers(sessionId: String): List<ExamAnswerEntity>

    @Query("SELECT * FROM exam_answers WHERE sessionId = :sessionId")
    fun observeAnswers(sessionId: String): Flow<List<ExamAnswerEntity>>
}
