package jp.co.testreason.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM quiz_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): SessionEntity?

    @Query("SELECT * FROM quiz_sessions WHERE isFinalized = 0 ORDER BY startedAt DESC LIMIT 1")
    fun getActiveSession(): Flow<SessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSession(session: SessionEntity)
}
