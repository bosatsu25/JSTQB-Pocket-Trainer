package jp.co.testreason.core.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.co.testreason.core.model.GlossaryTerm
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Supplies the glossary bundled with the app. Reads a file: call it off the main thread. */
fun interface GlossarySource {
    fun load(): List<GlossaryTerm>
}

class AssetGlossarySource @Inject constructor(
    @ApplicationContext private val context: Context
) : GlossarySource {

    override fun load(): List<GlossaryTerm> =
        GlossaryParser.parse(context.assets.open(GLOSSARY_FILE).bufferedReader().use { it.readText() })

    private companion object {
        const val GLOSSARY_FILE = "glossary/terms.json"
    }
}

/** Parses `assets/glossary/terms.json` (format: docs/content/AUTHORING_GUIDE.md). */
object GlossaryParser {

    private val json = Json { ignoreUnknownKeys = false }

    fun parse(text: String): List<GlossaryTerm> =
        json.decodeFromString<GlossaryFileDto>(text).terms.map { GlossaryTerm(it.id, it.term, it.chapters, it.summary) }
}

@Serializable
private data class GlossaryFileDto(val terms: List<GlossaryTermDto>)

@Serializable
private data class GlossaryTermDto(
    val id: String,
    val term: String,
    val chapters: List<Int>,
    val summary: String
)
