package jp.co.testreason.core.data

import jp.co.testreason.core.database.MasteryDao
import jp.co.testreason.core.database.MasteryEntity
import jp.co.testreason.core.model.ConfidenceLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MasteryRepositoryTest {

    private lateinit var masteryRepository: MasteryRepository
    private lateinit var fakeMasteryDao: FakeMasteryDao

    @Before
    fun setUp() {
        fakeMasteryDao = FakeMasteryDao()
        masteryRepository = MasteryRepositoryImpl(fakeMasteryDao, MutableTimeProvider(5_000L))
    }

    @Test
    fun updateMastery_calculatesCorrectScore() = runTest {
        masteryRepository.updateMastery("FL-1.1.1", true, ConfidenceLevel.HIGH)
        val masteries = masteryRepository.getAllMasteries().first()

        assertEquals(1, masteries.size)
        assertEquals("FL-1.1.1", masteries[0].learningObjectiveId)
        assertEquals(1.0f, masteries[0].score, 0.01f)
        assertEquals(1, masteries[0].totalAttempts)
        assertEquals(1, masteries[0].correctAttempts)
        assertEquals(5_000L, masteries[0].lastAttemptAt)
    }

    @Test
    fun updateMastery_guessedCorrectAnswerEarnsPartialCredit() = runTest {
        masteryRepository.updateMastery("FL-1.1.1", true, ConfidenceLevel.HIGH)
        masteryRepository.updateMastery("FL-1.1.1", true, ConfidenceLevel.GUESS)
        masteryRepository.updateMastery("FL-1.1.1", false, ConfidenceLevel.HIGH)

        val mastery = masteryRepository.getAllMasteries().first().single()
        assertEquals((1f + 0.25f + 0f) / 3, mastery.score, 0.001f)
        assertEquals(3, mastery.totalAttempts)
        assertEquals(2, mastery.correctAttempts)
    }
}

class FakeMasteryDao : MasteryDao {
    private val masteries = mutableMapOf<String, MasteryEntity>()

    override fun getAllMasteries() = flowOf(masteries.values.toList())
    override suspend fun getMasteryByLo(loId: String) = masteries[loId]
    override suspend fun insertOrUpdateMastery(mastery: MasteryEntity) {
        masteries[mastery.learningObjectiveId] = mastery
    }
}
