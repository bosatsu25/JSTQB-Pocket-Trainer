package jp.co.testreason.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import jp.co.testreason.core.model.MistakeReason
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts WHERE sessionId = :sessionId")
    fun getAttemptsBySession(sessionId: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE questionId = :questionId")
    fun getAttemptsByQuestion(questionId: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE sessionId = :sessionId AND questionId = :questionId")
    suspend fun getAttempt(sessionId: String, questionId: String): AttemptEntity?

    @Query("SELECT * FROM attempts")
    fun getAllAttempts(): Flow<List<AttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttemptIgnore(attempt: AttemptEntity): Long

    @Query("UPDATE attempts SET mistakeReason = :mistakeReason WHERE sessionId = :sessionId AND questionId = :questionId")
    suspend fun updateMistakeReasonOnly(sessionId: String, questionId: String, mistakeReason: MistakeReason)
}
