package io.github.petertrr.initbot

import io.github.petertrr.initbot.entities.Combatant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class Initiative(
    internal val members: MutableList<Combatant> = Collections.synchronizedList(mutableListOf()),
    private val random: Random = Random.Default
) {
    private val isInitiativeStarted = AtomicBoolean(false)
    private val currentCombatantIdx = AtomicInteger(-1)

    fun execute(rawCommand: String, fallbackName: String): CommandResult {
        val command = try {
            Command.parse(rawCommand, fallbackName)
        } catch (ex: Exception) {
            return Failure(ex)
        }
        return when (command) {
            Help -> help()
            Start -> start()
            End -> end()
            is Add -> add(command)
            is Remove -> {
                removeByName(command.name)
                Success("Successfully removed ${command.name} from initiative")
            }
            is Roll -> roll(command)
            is Countdown -> startCountdown(command)
            Round -> round()
        }
    }

    private fun help() = Success("""Available commands: start, end, add, remove, round, roll, next. See README.md for details""")

    internal fun start() =
        if (isInitiativeStarted.compareAndSet(false, true)) {
            members.clear()
            Success("Successfully started initiative, combatants should call `add` now")
        } else {
            Failure(IllegalStateException("Initiative already started, call `end` first"))
        }

    internal fun end() =
        if (isInitiativeStarted.compareAndSet(true, false)) {
            members.clear()
            Success("Initiative ended")
        } else {
            Failure(IllegalStateException("Initiative is not started, call `start` first"))
        }

    internal fun add(addCommand: Add): CommandResult {
        require(isInitiativeStarted.get()) { "Initiative is not started, run `start` first" }
        members.add(
            Combatant(addCommand.name, addCommand.baseModifier, null)
        )
        return Success("Added ${addCommand.name} with base modifier ${addCommand.baseModifier} to the initiative")
    }

    /**
     * @return a rolled value
     */
    internal fun roll(rollCommand: Roll): RollResult {
        val combatant = members.first { it.name == rollCommand.name }
        val roll = random.nextInt(1, 20)
        combatant.currentInitiative = roll
        return RollResult(roll, combatant.baseModifier + rollCommand.modifier, combatant.name)
    }

    /**
     * Returns a sequence of this round combatants, sorted by their initiative
     */
    internal fun round(): CommandResult {
        if (currentCombatantIdx.get() < 0) {
            // check if everyone has rolled before starting the round
            members.forEach { it.getCurrentInitiativeSafe() }
        }
        return if (currentCombatantIdx.compareAndSet(-1, 0) || currentCombatantIdx.compareAndSet(members.size, 0)) {
            RoundResult(
                members.sortedByDescending {
                    it.getCurrentInitiativeSafe()
                }
                    .asSequence()
            )
        } else {
            Failure(IllegalStateException("Current round is not finished yet, next combatant is ${members[currentCombatantIdx.get()].name}"))
        }
    }

    private fun startCountdown(countdown: Countdown): CommandResult {
        if (currentCombatantIdx.get() < 0) {
            // round is not started yet
            members.forEach { it.getCurrentInitiativeSafe() }
            currentCombatantIdx.set(0)
        }
        return if (currentCombatantIdx.get() < members.size - 1) {
            val idx = currentCombatantIdx.getAndIncrement()
            CountdownStarted(members[idx], countdown.seconds).also {
                if (idx == members.size - 1) {
                    // round is ended
                    members.forEach { it.currentInitiative = null }
                }
            }
        } else {
            Failure(IllegalStateException("All combatants have already acted in this round, please start the next one"))
        }
    }

    /**
     * Ends round, clearing all initiatives
     */
    internal fun endRound() = members.forEach {
        it.currentInitiative = null
    }

    internal fun removeByName(name: String) = members.removeIf { it.name == name }
}
