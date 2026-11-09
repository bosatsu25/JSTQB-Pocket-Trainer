package jp.co.testreason.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val learningObjectiveId: String,
    val stem: String,
    val choices: List<ChoiceEntity>,
    val explanation: String,
    @ColumnInfo(defaultValue = "1") val selectCount: Int = 1,
    val scenario: String? = null,
    @ColumnInfo(defaultValue = "0") val revision: Int = 0,
    /** Retired questions stay in the table so attempt history keeps its reference. */
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true
)

/** Lightweight projection used to decide which bundled questions need to be upserted. */
data class QuestionRevision(
    val id: String,
    val revision: Int,
    val isActive: Boolean
)
