package jp.co.testreason.core.ui.components

import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason

/** Learner-facing labels (the enum names are internal identifiers). */
val ConfidenceLevel.label: String
    get() = when (this) {
        ConfidenceLevel.GUESS -> "勘で選んだ"
        ConfidenceLevel.LOW -> "自信なし"
        ConfidenceLevel.MEDIUM -> "まあまあ"
        ConfidenceLevel.HIGH -> "自信あり"
    }

val MistakeReason.label: String
    get() = when (this) {
        MistakeReason.CONCEPT -> "概念の理解不足"
        MistakeReason.TERMINOLOGY -> "用語の混同"
        MistakeReason.MISREAD -> "問題の読み違い"
        MistakeReason.CARELESS -> "うっかりミス"
    }
