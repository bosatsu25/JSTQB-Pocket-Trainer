package jp.co.testreason.core.model

enum class ConfidenceLevel {
    GUESS,
    LOW,
    MEDIUM,
    HIGH
}

enum class MistakeReason {
    CONCEPT,
    TERMINOLOGY,
    MISREAD,
    CARELESS
}

enum class StudyMode {
    DAILY,
    CHAPTER,
    WEAKNESS,
    MOCK_EXAM,
    CUSTOM
}

enum class KLevel {
    K1,
    K2,
    K3
}
