package jp.co.testreason.core.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Supplies the questions bundled with the app. */
fun interface BundledQuestionSource {
    fun load(): List<BundledQuestion>
}

class AssetBundledQuestionSource @Inject constructor(
    @ApplicationContext private val context: Context
) : BundledQuestionSource {

    override fun load(): List<BundledQuestion> {
        val files = context.assets.list(QUESTIONS_DIR).orEmpty().filter { it.endsWith(".json") }.sorted()
        return files.flatMap { name ->
            val text = context.assets.open("$QUESTIONS_DIR/$name").bufferedReader().use { it.readText() }
            QuestionContentParser.parse(text).questions
        }
    }

    private companion object {
        const val QUESTIONS_DIR = "questions"
    }
}
