package jp.co.testreason.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        QuestionEntity::class,
        AttemptEntity::class,
        SessionEntity::class,
        MasteryEntity::class,
        ReviewScheduleEntity::class,
        QuestionCompletionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TestReasonDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun sessionDao(): SessionDao
    abstract fun masteryDao(): MasteryDao
    abstract fun reviewScheduleDao(): ReviewScheduleDao
    abstract fun questionCompletionDao(): QuestionCompletionDao
}
