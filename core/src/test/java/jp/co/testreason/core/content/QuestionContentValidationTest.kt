package jp.co.testreason.core.content

import jp.co.testreason.core.model.KLevel
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.model.Syllabus
import jp.co.testreason.core.ui.components.TABLE_SEPARATOR
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Validates the bundled question bank against docs/content/AUTHORING_GUIDE.md.
 * Runs on the JVM directly against src/main/assets so broken content fails the build.
 */
class QuestionContentValidationTest {

    private val questionDir = File("src/main/assets/questions")

    private val files: Map<String, QuestionFile> by lazy {
        questionDir.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
            .associate { it.name to QuestionContentParser.parse(it.readText()) }
    }

    private val active: List<BundledQuestion> by lazy {
        files.values.flatMap { it.questions }.filter { it.active }
    }

    @Test
    fun questionFiles_exist() {
        assertTrue("No question files in ${questionDir.absolutePath}", files.isNotEmpty())
    }

    @Test
    fun questions_followSchemaRules() {
        val errors = mutableListOf<String>()
        val allIds = files.values.flatMap { f -> f.questions.map { it.question.id } }
        allIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { errors += "duplicate id $it" }

        files.forEach { (name, file) ->
            file.questions.forEach { bundled -> errors += validate("$name/${bundled.question.id}", file.chapter, bundled.question) }
        }
        if (errors.isNotEmpty()) fail(errors.joinToString("\n"))
    }

    @Test
    fun everyLearningObjective_meetsMinimumQuestionCount() {
        val counts = active.groupingBy { it.question.learningObjectiveId }.eachCount()
        val report = Syllabus.learningObjectives.joinToString("\n") { lo ->
            val count = counts[lo.id] ?: 0
            val target = targetFor(lo.kLevel)
            "${lo.id} ${lo.kLevel} $count/$target" + if (count < target) "  <-- short" else ""
        }
        println("Question coverage (active=${active.size})\n$report")

        val short = Syllabus.learningObjectives.filter { (counts[it.id] ?: 0) < targetFor(it.kLevel) }
        assertTrue("LOs below target:\n" + short.joinToString("\n") { it.id }, short.isEmpty())
    }

    @Suppress("CyclomaticComplexMethod")
    private fun validate(where: String, chapter: Int, q: Question): List<String> {
        val errors = mutableListOf<String>()
        val lo = Syllabus.findLearningObjective(q.learningObjectiveId)
        if (!Regex("""c$chapter-\d{3}""").matches(q.id)) errors += "$where: id must be c$chapter-NNN"
        if (lo == null) {
            errors += "$where: unknown LO ${q.learningObjectiveId}"
        } else if (lo.chapter != chapter) {
            errors += "$where: LO ${lo.id} is not in chapter $chapter"
        }
        if (q.revision < 1) errors += "$where: revision must be >= 1"
        val expectedChoices = when (q.selectCount) {
            1 -> listOf("A", "B", "C", "D")
            2 -> listOf("A", "B", "C", "D", "E")
            else -> emptyList<String>().also { errors += "$where: selectCount must be 1 or 2" }
        }
        if (q.choices.map { it.id } != expectedChoices) errors += "$where: choices must be $expectedChoices"
        if (q.correctChoiceIds.size != q.selectCount) errors += "$where: correct count != selectCount"
        if (q.choices.map { it.text.trim() }.toSet().size != q.choices.size) errors += "$where: duplicate choice text"
        if (q.stem.isBlank() || q.explanation.isBlank()) errors += "$where: blank stem/explanation"
        q.choices.filter { it.text.isBlank() || it.distractorAnalysis.isBlank() }
            .forEach { errors += "$where: choice ${it.id} needs text and analysis" }
        if (lo?.kLevel == KLevel.K3 && q.scenario.isNullOrBlank()) errors += "$where: K3 question needs a scenario"
        if (q.scenario?.isBlank() == true) errors += "$where: scenario must be null or non-blank"
        if (!q.scenario.orEmpty().tablesHaveHeaderRows()) errors += "$where: every table needs a header row followed by |---|"
        val text = q.allText()
        BANNED_TERMS.filter { text.contains(it) }.forEach { errors += "$where: use syllabus terminology instead of 「$it」" }
        return errors
    }

    /**
     * The first row of a table is shown in bold and read to screen-reader users as the column names of every
     * other row, so it must really be a header: the line after it has to be the Markdown separator.
     * Lines inside a ``` code block are code, not table rows, as in `parseScenario`.
     */
    private fun String.tablesHaveHeaderRows(): Boolean {
        var inCode = false
        var previousWasTableRow = false
        val lines = lines().map { it.trim() }
        lines.forEachIndexed { index, line ->
            if (line.startsWith("```")) {
                inCode = !inCode
                previousWasTableRow = false
                return@forEachIndexed
            }
            val isTableRow = !inCode && line.startsWith("|")
            if (isTableRow && !previousWasTableRow && lines.getOrNull(index + 1)?.let { TABLE_SEPARATOR.matches(it) } != true) {
                return false
            }
            previousWasTableRow = isTableRow
        }
        return true
    }

    @Test
    fun tableHeaderRule_requiresASeparator_andIgnoresCodeBlocks() {
        assertTrue("| a | b |\n|---|---|\n| 1 | 2 |".tablesHaveHeaderRows())
        assertTrue("説明\n| a | b |\n|:--|--:|\n| 1 | 2 |\n続き".tablesHaveHeaderRows())
        assertFalse("header is missing", "| 1 | 2 |\n| 3 | 4 |".tablesHaveHeaderRows())
        assertFalse("second table has no header", "| a |\n|---|\n| 1 |\n文\n| 2 |\n| 3 |".tablesHaveHeaderRows())
        // A code line that starts with | is not a table.
        assertTrue("```\n| x | y\nif (a || b)\n```".tablesHaveHeaderRows())
        assertTrue("```\n| x |\n```\n| a |\n|---|\n| 1 |".tablesHaveHeaderRows())
    }

    private fun targetFor(kLevel: KLevel) = if (kLevel == KLevel.K3) K3_TARGET else K1_K2_TARGET

    private fun Question.allText(): String =
        (listOf(stem, explanation, scenario.orEmpty()) + choices.flatMap { listOf(it.text, it.distractorAnalysis) })
            .joinToString("\n")

    private companion object {
        const val K1_K2_TARGET = 3
        const val K3_TARGET = 6

        /** Wording replaced in 2023V4.0.J02 or not used by the syllabus. */
        val BANNED_TERMS = listOf("バグ", "不具合", "受入テスト", "開発者", "メトリック")
    }
}
