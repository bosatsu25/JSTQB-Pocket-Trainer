package jp.co.testreason.core.model

/**
 * A syllabus keyword with a one-line summary written by us (ADR-0006: not the syllabus or glossary wording).
 * A keyword listed in more than one chapter is a single term with several [chapters].
 */
data class GlossaryTerm(
    val id: String,
    val term: String,
    val chapters: List<Int>,
    val summary: String
)
