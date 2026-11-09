package jp.co.testreason.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ScenarioParserTest {

    @Test
    fun parsesParagraphTableAndCode() {
        val text = "会員ランクの仕様:\n| 金額 | ランク |\n|---|---|\n| 0〜9,999 | 一般 |\n| 10,000以上 | ゴールド |\n補足です。\n```\nif (a > 0) {\n  b()\n}\n```"

        val blocks = parseScenario(text)

        assertEquals(
            listOf(
                ScenarioBlock.Paragraph("会員ランクの仕様:"),
                ScenarioBlock.Table(listOf(listOf("金額", "ランク"), listOf("0〜9,999", "一般"), listOf("10,000以上", "ゴールド"))),
                ScenarioBlock.Paragraph("補足です。"),
                ScenarioBlock.Code("if (a > 0) {\n  b()\n}")
            ),
            blocks
        )
    }

    @Test
    fun alignedSeparatorRowIsSkipped() {
        val blocks = parseScenario("| a | b |\n|:--|--:|\n| 1 | 2 |")
        assertEquals(listOf(ScenarioBlock.Table(listOf(listOf("a", "b"), listOf("1", "2")))), blocks)
    }
}
