package io.github.petertrr.initbot.initiative

import io.github.petertrr.initbot.Success
import io.github.petertrr.initbot.entities.InitiativeMode
import io.github.petertrr.initbot.sorting.DescendantSorter

// TODO: Roll command w/o modifier
class RegularInitiative(turnDurationSeconds: Long) : Initiative(
    InitiativeMode.REGULAR,
    DescendantSorter(),
    turnDurationSeconds = turnDurationSeconds,
) {
    override fun endRound() = Success("Round ${currentRound.get()} has ended")
}