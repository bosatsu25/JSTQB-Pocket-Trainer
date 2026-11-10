package jp.co.testreason.core.database

import androidx.room.Entity
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason

@Entity(
    tableName = "attempts",
    primaryKeys = ["sessionId", "questionId"]
)
data class AttemptEntity(
    val id: String,
    val sessionId: String,
    val questionId: String,
    /**
     * Selected choice ids joined with "," in sorted order (e.g. "A" or "A,C").
     * The column name is kept from schema v1 to avoid a table rebuild.
     */
    val selectedChoiceId: String,
    val isCorrect: Boolean,
    val confidence: ConfidenceLevel?,
    val mistakeReason: MistakeReason?,
    val timeSpentMs: Long,
    val timestamp: Long
)
