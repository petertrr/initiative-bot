package io.github.petertrr.initbot

import io.github.petertrr.initbot.entities.Combatant

sealed class CommandResult

data class Success(val message: String) : CommandResult()

data class AddSuccess(val name: String, val baseModifier: Int) : CommandResult() {
    val message = "Added $name with base modifier $baseModifier to the initiative"
}

data class RemoveSuccess(val name: String) : CommandResult() {
    val message = "Removed $name from the initiative"
}

data class RollResult(val roll: Int, val modifier: Int, val name: String) : CommandResult() {
    val total = roll + modifier
}

data class Failure(val t: Throwable) : CommandResult()

data class CountdownStarted(val combatant: Combatant, val period: Long) : CommandResult()

data class RoundResult(
    val roundIdx: Int,
    val combatants: Sequence<Combatant>
) : CommandResult()
