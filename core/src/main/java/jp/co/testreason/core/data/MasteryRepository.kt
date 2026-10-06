package jp.co.testreason.core.data

import jp.co.testreason.core.database.MasteryDao
import jp.co.testreason.core.database.MasteryEntity
import jp.co.testreason.core.model.Mastery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface MasteryRepository {
    fun getAllMasteries(): Flow<List<Mastery>>
    suspend fun updateMastery(learningObjectiveId: String, isCorrect: Boolean)
}

@Singleton
class MasteryRepositoryImpl @Inject constructor(
    private val masteryDao: MasteryDao
) : MasteryRepository {

    override fun getAllMasteries(): Flow<List<Mastery>> {
        return masteryDao.getAllMasteries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateMastery(learningObjectiveId: String, isCorrect: Boolean) {
        val existing = masteryDao.getMasteryByLo(learningObjectiveId)
        val newTotal = (existing?.totalAttempts ?: 0) + 1
        val newCorrect = (existing?.correctAttempts ?: 0) + (if (isCorrect) 1 else 0)
        val newScore = newCorrect.toFloat() / newTotal

        val entity = MasteryEntity(
            learningObjectiveId = learningObjectiveId,
            score = newScore,
            lastAttemptAt = System.currentTimeMillis(),
            totalAttempts = newTotal,
            correctAttempts = newCorrect
        )
        masteryDao.insertOrUpdateMastery(entity)
    }
}

fun MasteryEntity.toDomain() = Mastery(
    learningObjectiveId = learningObjectiveId,
    score = score,
    lastAttemptAt = lastAttemptAt,
    totalAttempts = totalAttempts,
    correctAttempts = correctAttempts
)
