package jp.co.testreason.core.model

data class Choice(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val distractorAnalysis: String
)
