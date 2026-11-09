package jp.co.testreason.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTestReasonDatabase(
        @ApplicationContext context: Context
    ): TestReasonDatabase {
        return Room.databaseBuilder(
            context,
            TestReasonDatabase::class.java,
            "testreason.db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    fun provideQuestionDao(db: TestReasonDatabase): QuestionDao = db.questionDao()

    @Provides
    fun provideAttemptDao(db: TestReasonDatabase): AttemptDao = db.attemptDao()

    @Provides
    fun provideSessionDao(db: TestReasonDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideMasteryDao(db: TestReasonDatabase): MasteryDao = db.masteryDao()

    @Provides
    fun provideReviewScheduleDao(db: TestReasonDatabase): ReviewScheduleDao = db.reviewScheduleDao()

    @Provides
    fun provideQuestionCompletionDao(db: TestReasonDatabase): QuestionCompletionDao = db.questionCompletionDao()
}
