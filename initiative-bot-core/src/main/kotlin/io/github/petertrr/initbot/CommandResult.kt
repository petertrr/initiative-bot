package io.github.petertrr.initbot

import io.github.petertrr.initbot.entities.Combatant

sealed class CommandResult

data class Success(val message: String) : CommandResult()

data class RollResult(val roll: Int, val modifier: Int, val name: String) : CommandResult() {
    val total = roll + modifier
}

data class Failure(val t: Throwable) : CommandResult()

data class CountdownStarted(val combatant: Combatant, val period: Int) : CommandResult()

data class RoundResult(val combatants: Sequence<Combatant>) : CommandResult()
