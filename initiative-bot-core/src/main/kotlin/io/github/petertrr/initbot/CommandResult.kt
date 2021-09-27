package io.github.petertrr.initbot

import io.github.petertrr.initbot.entities.Combatant

sealed class CommandResult {
    open val message: String = TODO("Message not implemented for ${this::class}")
}

data class Success(override val message: String) : CommandResult()

data class AddSuccess(val name: String, val baseModifier: Int) : CommandResult() {
    override val message = "Added $name with base modifier $baseModifier to the initiative"
}

data class RemoveSuccess(val name: String) : CommandResult() {
    override val message = "Removed $name from the initiative"
}

data class RollResult(val roll: Int, val modifier: Int, val name: String) : CommandResult() {
    val total = roll + modifier
}

data class Failure(val t: Throwable, val rawCommand: String = "") : CommandResult() {
    override val message: String = "Error during command `$rawCommand`: `${t.javaClass.simpleName}: ${t.message}`"
}

data class CountdownStarted(val combatant: Combatant, val period: Long) : CommandResult()

data class RoundResult(
    val roundIdx: Int,
    val combatants: Sequence<Combatant>
) : CommandResult()

object EndSuccess: CommandResult() {
    override val message = "Initiative ended"
}
