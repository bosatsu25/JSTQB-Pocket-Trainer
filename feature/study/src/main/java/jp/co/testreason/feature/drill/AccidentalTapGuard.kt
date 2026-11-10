package jp.co.testreason.feature.drill

import jp.co.testreason.core.model.TimeProvider

/**
 * Ignores taps that arrive right after the content changed under the finger: where one tap replaces a button
 * with another control at the same place, the second tap of a double tap would act on the new control.
 */
class AccidentalTapGuard(private val timeProvider: TimeProvider, private val windowMs: Long = DEFAULT_WINDOW_MS) {

    private var armedAt: Long? = null

    /** Call when the content changes. */
    fun arm() {
        armedAt = timeProvider.currentTimeMillis()
    }

    /** True while a tap should be treated as accidental. A clock that moved backwards never blocks. */
    fun blocks(): Boolean {
        val sinceArmed = timeProvider.currentTimeMillis() - (armedAt ?: return false)
        return sinceArmed in 0 until windowMs
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 400L
    }
}
