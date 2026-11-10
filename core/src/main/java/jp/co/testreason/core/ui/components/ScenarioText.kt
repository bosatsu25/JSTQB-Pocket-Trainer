package jp.co.testreason.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Building blocks of a question scenario (see docs/content/AUTHORING_GUIDE.md). */
sealed interface ScenarioBlock {
    data class Paragraph(val text: String) : ScenarioBlock
    data class Table(val rows: List<List<String>>) : ScenarioBlock
    data class Code(val text: String) : ScenarioBlock
}

private val TABLE_SEPARATOR = Regex("""^\|?(\s*:?-{2,}:?\s*\|)+\s*:?-*:?\s*\|?$""")

/** Splits scenario text into paragraphs, Markdown-style tables and ``` code blocks. */
fun parseScenario(text: String): List<ScenarioBlock> {
    val blocks = mutableListOf<ScenarioBlock>()
    val paragraph = mutableListOf<String>()
    val table = mutableListOf<List<String>>()
    val code = mutableListOf<String>()
    var inCode = false

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) blocks += ScenarioBlock.Paragraph(paragraph.joinToString("\n"))
        paragraph.clear()
    }
    fun flushTable() {
        if (table.isNotEmpty()) blocks += ScenarioBlock.Table(table.toList())
        table.clear()
    }

    for (rawLine in text.lines()) {
        val line = rawLine.trimEnd()
        when {
            line.trimStart().startsWith("```") -> {
                if (inCode) {
                    blocks += ScenarioBlock.Code(code.joinToString("\n"))
                    code.clear()
                } else {
                    flushParagraph()
                    flushTable()
                }
                inCode = !inCode
            }
            inCode -> code += rawLine
            line.trimStart().startsWith("|") -> {
                flushParagraph()
                if (!TABLE_SEPARATOR.matches(line.trim())) {
                    table += line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                }
            }
            else -> {
                flushTable()
                paragraph += line
            }
        }
    }
    if (inCode && code.isNotEmpty()) blocks += ScenarioBlock.Code(code.joinToString("\n"))
    flushParagraph()
    flushTable()
    return blocks.filterNot { it is ScenarioBlock.Paragraph && it.text.isBlank() }
}

@Composable
fun ScenarioText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseScenario(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is ScenarioBlock.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodyMedium)
                is ScenarioBlock.Code -> CodeBlock(block.text)
                is ScenarioBlock.Table -> ScenarioTable(block.rows)
            }
        }
    }
}

@Composable
private fun CodeBlock(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        )
    }
}

/**
 * What a screen reader says for one table row. A cell on its own ("3%") means nothing without its column, so
 * each data row is read as one sentence that pairs every value with its header: "2行目。購入金額 0〜9,999、…".
 * The first row is the header row (Markdown tables always have one).
 */
fun tableRowDescription(rows: List<List<String>>, rowIndex: Int): String {
    fun cell(row: List<String>, column: Int) = row.getOrElse(column) { "" }.ifBlank { EMPTY_CELL }
    val columnCount = rows.maxOf { it.size }
    val header = rows.first()
    return if (rowIndex == 0) {
        "表の見出し。" + (0 until columnCount).joinToString("、") { cell(header, it) }
    } else {
        "${rowIndex + 1}行目。" + (0 until columnCount).joinToString("、") { "${cell(header, it)} ${cell(rows[rowIndex], it)}" }
    }
}

private const val EMPTY_CELL = "空欄"

@Composable
private fun ScenarioTable(rows: List<List<String>>) {
    val columnCount = rows.maxOf { it.size }
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        rows.forEachIndexed { rowIndex, row ->
            // One screen-reader node per row; the cells stay visible but are not read one by one.
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .clearAndSetSemantics { contentDescription = tableRowDescription(rows, rowIndex) }
            ) {
                for (column in 0 until columnCount) {
                    Text(
                        text = row.getOrElse(column) { "" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 64.dp, max = 220.dp)
                            .border(0.5.dp, borderColor)
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}
