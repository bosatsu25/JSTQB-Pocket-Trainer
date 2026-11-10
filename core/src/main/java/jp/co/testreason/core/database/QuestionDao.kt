package jp.co.testreason.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE isActive = 1")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: String): QuestionEntity?

    /** Retired questions included, in no particular order. Keep [ids] under SQLite's variable limit (999 on older devices). */
    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getQuestionsByIds(ids: List<String>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE learningObjectiveId = :loId AND isActive = 1")
    fun getQuestionsByLo(loId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE isActive = 1 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(limit: Int): List<QuestionEntity>

    @Query(
        "SELECT * FROM questions WHERE isActive = 1 AND id NOT IN (SELECT questionId FROM attempts) " +
            "ORDER BY RANDOM() LIMIT :limit"
    )
    suspend fun getUnseenQuestions(limit: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE isActive = 1 AND learningObjectiveId = :loId")
    suspend fun getActiveQuestionsByLo(loId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE isActive = 1 AND learningObjectiveId LIKE :loPrefix || '%'")
    suspend fun getActiveQuestionsByLoPrefix(loPrefix: String): List<QuestionEntity>

    @Query("SELECT id, revision, isActive FROM questions")
    suspend fun getQuestionRevisions(): List<QuestionRevision>

    @Query("SELECT COUNT(*) FROM questions WHERE isActive = 1")
    suspend fun countActiveQuestions(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("UPDATE questions SET isActive = 0 WHERE id IN (:ids)")
    suspend fun deactivateQuestions(ids: List<String>)
}
