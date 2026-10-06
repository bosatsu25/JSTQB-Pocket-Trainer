package jp.co.testreason.core.database

import androidx.room.Entity

@Entity(
    tableName = "question_completions",
    primaryKeys = ["sessionId", "questionId"]
)
data class QuestionCompletionEntity(
    val sessionId: String,
    val questionId: String,
    val completedAt: Long
)
