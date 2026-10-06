package jp.co.testreason.core.model

data class Question(
    val id: String,
    val learningObjectiveId: String,
    val stem: String,
    val choices: List<Choice>,
    val explanation: String
)
