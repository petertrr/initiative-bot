package io.github.petertrr.initbot.initiative

import io.github.petertrr.initbot.*
import io.github.petertrr.initbot.entities.Combatant
import io.github.petertrr.initbot.entities.InitiativeMode
import io.github.petertrr.initbot.entities.InitiativeMode.REGULAR
import io.github.petertrr.initbot.entities.InitiativeMode.SPEED_FACTOR
import io.github.petertrr.initbot.sorting.CombatantsSorter
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

abstract class Initiative(
    private val mode: InitiativeMode,
    private val sorter: CombatantsSorter,
    internal val members: MutableList<Combatant> = Collections.synchronizedList(mutableListOf()),
    private val random: Random = Random.Default,
    private val turnDurationSeconds: Long = 45L
) {
    private val isInitiativeStarted = AtomicBoolean(false)
    internal val isRoundStarted = AtomicBoolean(false)
    private val currentCombatantIdx = AtomicInteger(0)
    protected val currentRound = AtomicInteger(0)

    fun hasNextCombatant(): Boolean = currentCombatantIdx.get() <= members.lastIndex

    fun execute(rawCommand: String, fallbackName: String): CommandResult {
        val command = try {
            Command.parse(rawCommand, fallbackName, turnDurationSeconds)
        } catch (ex: Exception) {
            return Failure(ex, rawCommand)
        }
        return when (command) {
            Help -> help()
            is Start -> start()
            End -> end()
            is Add -> add(command)
            is Remove -> removeByName(command.name)
            is Roll -> roll(command)
            is Countdown -> startCountdown(command)
            Round -> round()
            EndRound -> endRound()
        }.let {
            if (it is Failure) it.copy(rawCommand = rawCommand)
            else it
        }
    }

    // todo: update help ans README
    private fun help() = Success("""Available commands: start, end, add, remove, round, roll, next. See README.md for details""")

    internal fun start() =
        if (isInitiativeStarted.compareAndSet(false, true)) {
            members.clear()
            Success("Successfully started initiative with mode $mode, combatants should call `add` now")
        } else {
            Failure(IllegalStateException("Initiative already started, call `end` first"))
        }

    internal fun end() =
        if (isInitiativeStarted.compareAndSet(true, false)) {
            members.clear()
            EndSuccess
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
            // sort members *in place*
            sorter.sort(members)
            currentCombatantIdx.set(0)
            currentRound.incrementAndGet()
            RoundResult(currentRound.get(), members.asSequence())
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
    internal abstract fun endRound(): CommandResult

    internal fun removeByName(name: String) =
        if (members.removeIf { it.name == name }) {
            RemoveSuccess(name)
        } else {
            Failure(IllegalStateException("Character $name is not present in the initiative"))
        }

    protected fun <T : CommandResult> withRollbackAtomic(a: AtomicBoolean, op: () -> T): CommandResult {
        val oldValue = a.get()
        return try {
            op()
        } catch (t: Throwable) {
            a.set(oldValue)
            Failure(t)
        }
    }

    companion object {
        @JvmStatic
        fun create(mode: InitiativeMode, turnDurationSeconds: Long) = when (mode) {
            SPEED_FACTOR -> SpeedFactorInitiative(turnDurationSeconds)
            REGULAR -> RegularInitiative(turnDurationSeconds)
        }
    }
}
