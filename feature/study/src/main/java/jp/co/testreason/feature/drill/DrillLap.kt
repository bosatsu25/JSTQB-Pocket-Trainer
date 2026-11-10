package jp.co.testreason.feature.drill

/**
 * One lap of the rapid drill (周回モード): every question is asked until it has been answered correctly once.
 * A wrong answer, or looking at the answer without answering, sends the question to the end of the queue.
 *
 * Pure and immutable so it can be unit-tested and saved/restored as plain lists.
 */
data class DrillLap(
    /** Every question of this lap. */
    val questionIds: List<String>,
    /** Questions still to be answered correctly; the head is the current one. */
    val queue: List<String> = questionIds,
    /** Questions answered wrong (or revealed) at least once in this lap. */
    val missed: Set<String> = emptySet(),
    /** Number of answers given, including reveals. */
    val answerCount: Int = 0
) {
    val current: String? get() = queue.firstOrNull()
    val isFinished: Boolean get() = queue.isEmpty()
    val total: Int get() = questionIds.size
    val clearedCount: Int get() = total - queue.size

    /** Questions answered correctly the first time they were shown. */
    val firstTryCorrectCount: Int get() = (questionIds.toSet() - missed).size - queue.count { it !in missed }

    /** Records the answer to the current question and moves on. */
    fun answered(correct: Boolean): DrillLap {
        val id = current ?: return this
        val rest = queue.drop(1)
        return if (correct) {
            copy(queue = rest, answerCount = answerCount + 1)
        } else {
            copy(queue = rest + id, missed = missed + id, answerCount = answerCount + 1)
        }
    }

    companion object {
        fun start(questionIds: List<String>) = DrillLap(questionIds = questionIds.distinct())
    }
}
