package io.github.petertrr.initbot.sorting

import io.github.petertrr.initbot.entities.Combatant
import kotlin.random.Random
import kotlin.random.nextInt

class DescendantSorter(private val random: Random = Random.Default) : CombatantsSorter {
    override fun sort(combatants: MutableList<Combatant>) {
        combatants.sortWith { a, b ->
            (b.getCurrentInitiativeSafe() - a.getCurrentInitiativeSafe()).let { initDiff ->
                if (initDiff == 0){
                    (b.baseModifier - a.baseModifier).let { modDiff ->
                        if (modDiff == 0) {
                            // in this case, choose randomly
                            random.nextInt(-10..10)
                        } else {
                            modDiff
                        }
                    }
                } else {
                    initDiff
                }
            }
        }
    }
}
