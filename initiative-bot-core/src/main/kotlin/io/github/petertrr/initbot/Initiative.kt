package io.github.petertrr.initbot

import io.github.petertrr.initbot.entities.Combatant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class Initiative(
    internal val members: MutableList<Combatant> = Collections.synchronizedList(mutableListOf()),
    private val random: Random = Random.Default,
    private val turnDurationSeconds: Long = 45L
) {
    private val isInitiativeStarted = AtomicBoolean(false)
    internal val isRoundStarted = AtomicBoolean(false)
    private val currentCombatantIdx = AtomicInteger(0)

    fun hasNextCombatant(): Boolean = currentCombatantIdx.get() <= members.lastIndex

    fun execute(rawCommand: String, fallbackName: String): CommandResult {
        val command = try {
            Command.parse(rawCommand, fallbackName, turnDurationSeconds)
        } catch (ex: Exception) {
            return Failure(ex)
        }
        return when (command) {
            Help -> help()
            Start -> start()
            End -> end()
            is Add -> add(command)
            is Remove -> removeByName(command.name)
            is Roll -> roll(command)
            is Countdown -> startCountdown(command)
            Round -> round()
            EndRound -> endRound()
        }
    }

    // todo: update help ans README
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
        require(!isRoundStarted.get()) { "Round is already started, new combatants can be added only between rounds" }
        members.add(
            Combatant(addCommand.name, addCommand.baseModifier, null)
        )
        return AddSuccess(addCommand.name, addCommand.baseModifier)
    }

    /**
     * @return a rolled value
     */
    internal fun roll(rollCommand: Roll): RollResult {
        require(!isRoundStarted.get()) { "Round is already started, initiative can be rolled only between rounds" }
        val combatant = members.first { it.name == rollCommand.name }
        val roll = random.nextInt(1, 20)
        combatant.currentInitiative = roll + combatant.baseModifier + rollCommand.modifier
        return RollResult(roll, combatant.baseModifier + rollCommand.modifier, combatant.name)
    }

    /**
     * Returns a sequence of this round combatants, sorted by their initiative
     */
    internal fun round(): CommandResult = withRollbackAtomic(isRoundStarted) {
        if (!isRoundStarted.compareAndSet(false, true)) {
            Failure(IllegalStateException("Round is already in progress, next combatant is ${members[currentCombatantIdx.get()].name}. To finish round, call `next` for all participants and then `end-round` before starting the new one"))
        } else {
            currentCombatantIdx.set(0)
            // sort members according to current initiative *in place*
            members.sortByDescending {
                it.getCurrentInitiativeSafe()
            }
            RoundResult(members.asSequence())
        }
    }

    internal fun startCountdown(countdown: Countdown): CommandResult {
        require(isRoundStarted.get()) { "Round is not started, can't execute `next`" }
        return if (currentCombatantIdx.get() < members.size) {
            val idx = currentCombatantIdx.getAndIncrement()
            CountdownStarted(members[idx], countdown.seconds)
        } else {
            Failure(IllegalStateException("All combatants have already acted in this round, please start the next one"))
        }
    }

    /**
     * Ends round, clearing all initiatives
     */
    internal fun endRound() = withRollbackAtomic(isRoundStarted) {
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

    internal fun removeByName(name: String) =
        if (members.removeIf { it.name == name }) {
            RemoveSuccess(name)
        } else {
            Failure(IllegalStateException("Character $name is not present in the initiative"))
        }

    private fun <T : CommandResult> withRollbackAtomic(a: AtomicBoolean, op: () -> T): CommandResult {
        val oldValue = a.get()
        return try {
            op()
        } catch (t: Throwable) {
            a.set(oldValue)
            Failure(t)
        }
    }
}
