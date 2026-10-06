package jp.co.testreason.core.model

data class Attempt(
    val id: String,
    val sessionId: String,
    val questionId: String,
    val selectedChoiceId: String,
    val isCorrect: Boolean,
    val confidence: ConfidenceLevel?,
    val mistakeReason: MistakeReason?,
    val timeSpentMs: Long,
    val timestamp: Long
)
