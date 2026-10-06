package jp.co.testreason.core.model

data class LearningObjective(
    val id: String,
    val chapterId: Int,
    val title: String,
    val description: String,
    val kLevel: KLevel
)
