package jp.co.testreason.core.model

data class QuizSession(
    val id: String,
    val mode: StudyMode,
    val questionIds: List<String>,
    val currentQuestionIndex: Int,
    val startedAt: Long,
    val completedAt: Long?,
    val isFinalized: Boolean
)
