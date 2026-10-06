package jp.co.testreason.core.data

import jp.co.testreason.core.database.MasteryDao
import jp.co.testreason.core.database.MasteryEntity
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
        masteryRepository = MasteryRepositoryImpl(fakeMasteryDao)
    }

    @Test
    fun updateMastery_calculatesCorrectScore() = runTest {
        masteryRepository.updateMastery("FL-1.1.1", true)
        val masteries = masteryRepository.getAllMasteries().first()

        assertEquals(1, masteries.size)
        assertEquals("FL-1.1.1", masteries[0].learningObjectiveId)
        assertEquals(1.0f, masteries[0].score, 0.01f)
        assertEquals(1, masteries[0].totalAttempts)
        assertEquals(1, masteries[0].correctAttempts)
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
