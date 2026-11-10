package jp.co.testreason.core.model

/**
 * A learning objective of the JSTQB Foundation Level syllabus.
 *
 * [summary] is our own short description (ADR-0006); it is not the official LO sentence.
 */
data class LearningObjective(
    val id: String,
    val kLevel: KLevel,
    val summary: String
) {
    /** "FL-4.2.2" -> "4.2.2" */
    val section: String get() = id.removePrefix("FL-")

    val chapter: Int get() = section.substringBefore('.').toInt()
}

data class SyllabusChapter(
    val number: Int,
    val title: String,
    val studyMinutes: Int
)
