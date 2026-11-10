package jp.co.testreason.core.model

data class Attempt(
    val id: String,
    val sessionId: String,
    val questionId: String,
    /** Sorted ids of the selected choices (one element for single-answer questions). */
    val selectedChoiceIds: List<String>,
    val isCorrect: Boolean,
    val confidence: ConfidenceLevel?,
    val mistakeReason: MistakeReason?,
    val timeSpentMs: Long,
    val timestamp: Long
)
