package jp.co.testreason.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query("SELECT * FROM questions WHERE learningObjectiveId = :loId")
    fun getQuestionsByLo(loId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions LIMIT :limit")
    suspend fun getRandomQuestions(limit: Int): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)
}
