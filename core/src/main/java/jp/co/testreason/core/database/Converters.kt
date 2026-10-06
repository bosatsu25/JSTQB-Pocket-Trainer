package jp.co.testreason.core.database

import androidx.room.TypeConverter
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.StudyMode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromConfidence(value: ConfidenceLevel?): String? = value?.name

    @TypeConverter
    fun toConfidence(value: String?): ConfidenceLevel? = value?.let { ConfidenceLevel.valueOf(it) }

    @TypeConverter
    fun fromMistakeReason(value: MistakeReason?): String? = value?.let { it.name }

    @TypeConverter
    fun toMistakeReason(value: String?): MistakeReason? = value?.let { MistakeReason.valueOf(it) }

    @TypeConverter
    fun fromStudyMode(value: StudyMode): String = value.name

    @TypeConverter
    fun toStudyMode(value: String): StudyMode = StudyMode.valueOf(value)

    @TypeConverter
    fun fromChoiceList(value: List<ChoiceEntity>): String = json.encodeToString(value)

    @TypeConverter
    fun toChoiceList(value: String): List<ChoiceEntity> = json.decodeFromString(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)
}

@kotlinx.serialization.Serializable
data class ChoiceEntity(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val distractorAnalysis: String
)
