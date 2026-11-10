package jp.co.testreason.core.content

import jp.co.testreason.core.model.GlossaryTerm
import jp.co.testreason.core.model.Syllabus
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/** Validates the bundled glossary; runs on the JVM directly against src/main/assets. */
class GlossaryContentValidationTest {

    private val terms: List<GlossaryTerm> by lazy {
        GlossaryParser.parse(File("src/main/assets/glossary/terms.json").readText())
    }

    @Test
    fun everyChapter_hasItsSyllabusKeywordCount() {
        val perChapter = Syllabus.chapters.associate { chapter -> chapter.number to terms.count { chapter.number in it.chapters } }
        assertEquals(KEYWORDS_PER_CHAPTER, perChapter)
    }

    @Test
    fun terms_followTheFormatRules() {
        val errors = mutableListOf<String>()
        terms.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys.forEach { errors += "duplicate id $it" }
        terms.groupingBy { it.term }.eachCount().filterValues { it > 1 }.keys.forEach { errors += "duplicate term $it" }
        terms.forEach { term ->
            val where = "${term.id} ${term.term}"
            if (!Regex("""g-\d{3}""").matches(term.id)) errors += "$where: id must be g-NNN"
            if (term.term.isBlank()) errors += "$where: blank term"
            if (term.chapters.isEmpty() || term.chapters.any { it !in 1..Syllabus.chapters.size }) errors += "$where: bad chapters"
            if (term.chapters != term.chapters.distinct().sorted()) errors += "$where: chapters must be sorted and unique"
            val summary = term.summary
            if (summary.isBlank() || '\n' in summary || !summary.endsWith("。")) errors += "$where: summary must be one line ending with 。"
            if (summary.length > MAX_SUMMARY_LENGTH) errors += "$where: summary longer than $MAX_SUMMARY_LENGTH characters"
            BANNED_TERMS.filter { summary.contains(it) }.forEach { errors += "$where: use syllabus terminology instead of 「$it」" }
        }
        if (errors.isNotEmpty()) fail(errors.joinToString("\n"))
    }

    /**
     * Keywords are examinable at K1 (syllabus 0.5). Reports which keywords no bundled question mentions,
     * and fails when a keyword that used to be mentioned no longer is.
     */
    @Test
    fun keywords_areMentionedByTheQuestionBank() {
        val questionText = File("src/main/assets/questions").listFiles { f -> f.extension == "json" }.orEmpty()
            .flatMap { QuestionContentParser.parse(it.readText()).questions }
            .filter { it.active }
            .joinToString("\n") { bundled ->
                val q = bundled.question
                (listOf(q.stem, q.explanation, q.scenario.orEmpty()) + q.choices.flatMap { listOf(it.text, it.distractorAnalysis) })
                    .joinToString("\n")
            }
        val unmentioned = terms.map { it.term }.filterNot { questionText.contains(it) }.toSet()
        println("Keyword coverage: ${terms.size - unmentioned.size}/${terms.size} mentioned; not mentioned: $unmentioned")

        assertEquals(
            "Update KNOWN_UNMENTIONED_KEYWORDS (shrink it when a question now covers a keyword)",
            KNOWN_UNMENTIONED_KEYWORDS,
            unmentioned
        )
    }

    private companion object {
        /** Keywords that appear in no question text as of this revision of the bank (the concepts may still be tested). */
        val KNOWN_UNMENTIONED_KEYWORDS = setOf("コラボレーションベースのテストアプローチ", "状態遷移テスト", "リスクマネジメント")

        /** Number of keywords listed at the top of each chapter of syllabus 2023V4.0.J02. */
        val KEYWORDS_PER_CHAPTER = mapOf(1 to 28, 2 to 17, 3 to 10, 4 to 18, 5 to 25, 6 to 1)
        const val MAX_SUMMARY_LENGTH = 60
        val BANNED_TERMS = listOf("バグ", "不具合", "受入テスト", "開発者", "メトリック")
    }
}
