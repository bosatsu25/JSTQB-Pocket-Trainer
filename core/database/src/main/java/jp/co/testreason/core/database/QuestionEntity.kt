package jp.co.testreason.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val learningObjectiveId: String,
    val stem: String,
    val choices: List<ChoiceEntity>,
    val explanation: String
)
