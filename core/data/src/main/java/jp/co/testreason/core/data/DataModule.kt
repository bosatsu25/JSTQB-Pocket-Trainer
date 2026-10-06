package jp.co.testreason.core.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.co.testreason.core.model.SystemTimeProvider
import jp.co.testreason.core.model.TimeProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindQuizRepository(
        impl: QuizRepositoryImpl
    ): QuizRepository

    @Binds
    @Singleton
    abstract fun bindMasteryRepository(
        impl: MasteryRepositoryImpl
    ): MasteryRepository

    companion object {
        @Provides
        @Singleton
        fun provideTimeProvider(): TimeProvider = SystemTimeProvider()
    }
}
