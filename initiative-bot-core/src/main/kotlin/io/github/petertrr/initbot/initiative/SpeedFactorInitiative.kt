package io.github.petertrr.initbot.initiative

import io.github.petertrr.initbot.Failure
import io.github.petertrr.initbot.Success
import io.github.petertrr.initbot.entities.InitiativeMode
import io.github.petertrr.initbot.sorting.DescendantSorter

class SpeedFactorInitiative(turnDurationSeconds: Long) : Initiative(
    InitiativeMode.SPEED_FACTOR,
    DescendantSorter(),
    turnDurationSeconds = turnDurationSeconds,
) {
    override fun endRound() = withRollbackAtomic(isRoundStarted) {
        if (isRoundStarted.compareAndSet(true, false)) {
            members.forEach {
                it.currentInitiative = null
            }
            Success(
                """|Round ended, combatants can roll new initiative now.
                   |Choose your action and bonus action, apply modifiers (only once, if both have same nature):
                   |- Spellcasting: subtract spell level
                   |- Melee, heavy weapon: -2
                   |- Melee, light or finesse weapon: +2
                   |- Melee, two-handed: -2
                   |- Ranged, loading: -5
                   |If you are not Medium-sized, apply the appropriate size modifier.
                """.trimMargin()
            )
        } else {
            Failure(IllegalStateException("Round is not started, call `round` first"))
        }
    }
}