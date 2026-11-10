package jp.co.testreason.core.model

data class Question(
    val id: String,
    val learningObjectiveId: String,
    val stem: String,
    val choices: List<Choice>,
    val explanation: String,
    /** Number of choices the learner must select (1, or 2 for 「2つ選べ」). */
    val selectCount: Int = 1,
    /** Optional precondition text (specification, table, state model) shown above the stem. */
    val scenario: String? = null,
    /** Content revision; incremented whenever the bundled text or answer changes. */
    val revision: Int = 0
) {
    val correctChoiceIds: Set<String> get() = choices.filter { it.isCorrect }.map { it.id }.toSet()

    val learningObjective: LearningObjective? get() = Syllabus.findLearningObjective(learningObjectiveId)

    /** Only an exact match of the correct set is correct; partial selections are wrong. */
    fun isCorrectSelection(selectedChoiceIds: Collection<String>): Boolean =
        selectedChoiceIds.toSet() == correctChoiceIds
}
