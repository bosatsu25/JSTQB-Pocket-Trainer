package jp.co.testreason.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuestionCompletionDao {
    @Query("SELECT * FROM question_completions WHERE sessionId = :sessionId AND questionId = :questionId")
    suspend fun getCompletion(sessionId: String, questionId: String): QuestionCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletionIgnore(completion: QuestionCompletionEntity): Long
}
