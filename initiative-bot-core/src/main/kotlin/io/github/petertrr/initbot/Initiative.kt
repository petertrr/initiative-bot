package io.github.petertrr.initbot

import kotlin.random.Random

class Initiative(
    private val members: MutableMap<String, Int?> = mutableMapOf(),
    private val random: Random = Random.Default
) {
    /**
     * Add a combatant with [name] and [modifier]
     *
     * @return a rolled value
     */
    fun roll(name: String, modifier: Int): Int {
        val roll = random.nextInt(1, 20)
        members[name] = roll + modifier
        return roll
    }

    /**
     * Returns a sequence of this round combatants, sorted by their initiative
     */
    fun round() = members.keys
        .sortedBy {
            requireNotNull(members[it]) { "Combatant $it has unset initiative, but new round has been requested" }
            members[it]!!
        }
        .asSequence()

    /**
     * Ends round, clearing all initiatives
     */
    fun endRound() = members.keys.forEach {
        members.compute(it) { _, _ -> null }
    }

    fun remove(name: String) = members.remove(name)
}
