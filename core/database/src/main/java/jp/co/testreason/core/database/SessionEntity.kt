package jp.co.testreason.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import jp.co.testreason.core.model.StudyMode

@Entity(tableName = "quiz_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val mode: StudyMode,
    val questionIds: List<String>,
    val currentQuestionIndex: Int,
    val startedAt: Long,
    val completedAt: Long?,
    val isFinalized: Boolean
)
