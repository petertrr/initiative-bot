package io.github.petertrr.initbot.sorting

import io.github.petertrr.initbot.entities.Combatant

/**
 * Provided list of combatants, sort them *in place* according to a particular initiative rules
 */
fun interface CombatantsSorter {
    fun sort(combatants: MutableList<Combatant>)
}
