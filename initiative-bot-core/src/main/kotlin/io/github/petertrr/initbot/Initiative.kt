package io.github.petertrr.initbot

import kotlin.random.Random

class Initiative(
    private val members: MutableMap<String, Int?> = mutableMapOf(),
    private val random: Random = Random.Default
) {
    fun execute(rawCommand: String): CommandResult {
        val command = try {
            Command.parse(rawCommand)
        } catch (ex: Exception) {
            return Failure(ex)
        }
        return when (command) {
            Help -> help()
            Start -> start()
            End -> end()
            is Add -> TODO()
            is Remove -> {
                remove(command.name)
                Success("Successfully removed ${command.name} from initiative")
            }
            is Roll -> RollResult(roll(command.name, command.modifier), command.modifier)
            is Countdown -> CountdownStarted(command.seconds)
            Round -> TODO()  // round()
        }
    }

    fun isEmpty() = members.isEmpty()

    private fun help() = Success("""Available commands: start, end, add, remove, round, roll, next. See README.md for details""")

    internal fun start() =
        if (isEmpty()) {
            members.clear()
            Success("Successfully started initiative")
        } else {
            Failure(IllegalStateException("Initiative already started, call `end` first"))
        }

    internal fun end() =
        if (isEmpty()) {
            Failure(IllegalStateException("Initiative is not started, call `start` first"))
        } else {
            members.clear()
            Success("Initiative ended")
        }

    /**
     * Add a combatant with [name] and [modifier]
     *
     * @return a rolled value
     */
    internal fun roll(name: String, modifier: Int): Int {
        val roll = random.nextInt(1, 20)
        members[name] = roll + modifier
        return roll
    }

    /**
     * Returns a sequence of this round combatants, sorted by their initiative
     */
    internal fun round() = members.keys
        .sortedBy {
            requireNotNull(members[it]) { "Combatant $it has unset initiative, but new round has been requested" }
            members[it]!!
        }
        .asSequence()

    /**
     * Ends round, clearing all initiatives
     */
    internal fun endRound() = members.keys.forEach {
        members.compute(it) { _, _ -> null }
    }

    internal fun remove(name: String) = members.remove(name)
}
