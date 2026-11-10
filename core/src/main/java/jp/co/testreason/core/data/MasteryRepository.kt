package jp.co.testreason.core.data

import jp.co.testreason.core.database.MasteryDao
import jp.co.testreason.core.database.MasteryEntity
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.Mastery
import jp.co.testreason.core.model.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface MasteryRepository {
    fun getAllMasteries(): Flow<List<Mastery>>

    /**
     * Records one completed answer for an LO. [Mastery.score] is the average credit per answer:
     * a correct answer earns full credit only when it was not a guess (see [MasteryCredit]).
     */
    suspend fun updateMastery(learningObjectiveId: String, isCorrect: Boolean, confidence: ConfidenceLevel?)
}

/** Credit a single answer contributes to mastery (docs/ux/LEARNING_LOOP.md). */
object MasteryCredit {
    fun of(isCorrect: Boolean, confidence: ConfidenceLevel?): Float = when {
        !isCorrect -> 0f
        confidence == ConfidenceLevel.GUESS -> 0.25f
        confidence == ConfidenceLevel.LOW -> 0.5f
        confidence == null -> 0.75f
        else -> 1f
    }
}

@Singleton
class MasteryRepositoryImpl @Inject constructor(
    private val masteryDao: MasteryDao,
    private val timeProvider: TimeProvider
) : MasteryRepository {

    override fun getAllMasteries(): Flow<List<Mastery>> {
        return masteryDao.getAllMasteries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateMastery(learningObjectiveId: String, isCorrect: Boolean, confidence: ConfidenceLevel?) {
        val existing = masteryDao.getMasteryByLo(learningObjectiveId)
        val previousTotal = existing?.totalAttempts ?: 0
        val previousCredit = (existing?.score ?: 0f) * previousTotal
        val newTotal = previousTotal + 1
        val newCorrect = (existing?.correctAttempts ?: 0) + (if (isCorrect) 1 else 0)

        val entity = MasteryEntity(
            learningObjectiveId = learningObjectiveId,
            score = (previousCredit + MasteryCredit.of(isCorrect, confidence)) / newTotal,
            lastAttemptAt = timeProvider.currentTimeMillis(),
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
