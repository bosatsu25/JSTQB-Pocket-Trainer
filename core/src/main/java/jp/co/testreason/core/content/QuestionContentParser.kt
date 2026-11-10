package jp.co.testreason.core.content

import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.Question
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Parses one bundled question file (format: docs/content/AUTHORING_GUIDE.md). */
object QuestionContentParser {

    private val json = Json { ignoreUnknownKeys = false }

    fun parse(text: String): QuestionFile {
        val dto = json.decodeFromString<QuestionFileDto>(text)
        return QuestionFile(
            chapter = dto.chapter,
            questions = dto.questions.map { it.toBundled() }
        )
    }

    private fun QuestionDto.toBundled() = BundledQuestion(
        question = Question(
            id = id,
            learningObjectiveId = lo,
            stem = stem,
            choices = choices.map { Choice(it.id, it.text, it.correct, it.analysis) },
            explanation = explanation,
            selectCount = selectCount,
            scenario = scenario,
            revision = revision
        ),
        active = active
    )
}

data class QuestionFile(
    val chapter: Int,
    val questions: List<BundledQuestion>
)

data class BundledQuestion(
    val question: Question,
    val active: Boolean
)

@Serializable
private data class QuestionFileDto(
    val chapter: Int,
    val questions: List<QuestionDto>
)

@Serializable
private data class QuestionDto(
    val id: String,
    val lo: String,
    val revision: Int,
    val selectCount: Int = 1,
    val scenario: String? = null,
    val stem: String,
    val choices: List<ChoiceDto>,
    val explanation: String,
    val active: Boolean = true
)

@Serializable
private data class ChoiceDto(
    val id: String,
    val text: String,
    val correct: Boolean,
    val analysis: String
)
