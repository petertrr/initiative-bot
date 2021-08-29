package io.github.petertrr.initbot

import io.github.petertrr.initbot.entities.Combatant
import io.github.petertrr.initbot.sorting.CombatantsSorter
import io.github.petertrr.initbot.sorting.DescendantSorter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.random.Random

@ExtendWith(MockitoExtension::class)
class InitiativeTest {
    @Mock lateinit var random: Random
    private lateinit var sorter: CombatantsSorter

    @BeforeEach
    fun setUp() {
        sorter = DescendantSorter(random)
    }

    @Test
    fun `default initiative flow`() {
        Mockito.`when`(random.nextInt(1, 20))
            .thenReturn(5, 3, 5)
        val initiative = Initiative(sorter = sorter, random = random)
        val defaultName = "Fallback"

        initiative.executeCommands(
            defaultName = defaultName,
            "start",
            "add 0 Jerry",
            "add -2 Tom",
            "add 0 Tuffy",
        )

        Assertions.assertIterableEquals(
            listOf(Combatant("Jerry", 0), Combatant("Tom", -2), Combatant("Tuffy", 0)),
            initiative.members
        )

        // first round
        initiative.executeCommands(
            defaultName = defaultName,
            "roll 0 Tom",
            "roll +0 Jerry",
            "roll 2 Tuffy",
        )

        Assertions.assertIterableEquals(
            listOf(
                Combatant("Tuffy", 0, 7),
                Combatant("Jerry", 0, 3),
                Combatant("Tom", -2, 3),
            ),
            (initiative.round() as RoundResult).combatants.asIterable()
        )

        initiative.executeCommands(
            defaultName = defaultName,
            "next",
            "next",
            "next",
            "end-round",
        )

        Assertions.assertFalse(initiative.isRoundStarted.get()) {
            "Should have marked round as ended after `end-round` command"
        }

        // second round
        initiative.executeCommands(
            defaultName = defaultName,
            "roll 1 Tom",
            "roll -2 Jerry",
            "roll 3 Tuffy",
            "round",
            "next",
            "next",
            "next",
            "end",
        )

        Assertions.assertTrue(initiative.members.isEmpty())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `test with 6 members and removing before round`() {
        Mockito.`when`(random.nextInt(1, 20))
            .thenReturn(17, 14, 10, 10, 15, 7)
        val initiative = Initiative(sorter = sorter, random = random)
        initiative.executeCommands(
            defaultName = "Fallback",
            "start",
            "add +4 Tom",
            "add +3 Jerry",
            "add +1 Tuffy",
            "add +2 Alpha",
            "add +1 Beta",
            "add +3 Gamma",
            "remove Tuffy",
            "add +2 Tuffy",
            "roll -2 Beta",
            "roll +0 Gamma",
            "roll +1 Tuffy",
            "roll +0 Jerry",
            "roll +2 Alpha",
            "roll -5 Tom",
        )

        val combatants = (initiative.round() as RoundResult).combatants.toList()
        Assertions.assertIterableEquals(
            listOf("Alpha", "Gamma", "Beta", "Jerry", "Tuffy", "Tom"),
            combatants.map { it.name }
        )

        Assertions.assertIterableEquals(
            listOf(19, 17, 16, 13, 13, 6),
            combatants.map { it.currentInitiative }
        )

        val countdown = Countdown(60)
        val combatantsSorted = buildList {
            // fixme: currently after last participant's turn all initiatives are set to null, so for now 5 instead of 6
            repeat(5) { add((initiative.startCountdown(countdown) as CountdownStarted).combatant) }
        }
        Assertions.assertTrue {
            combatantsSorted.zipWithNext().all { it.first.currentInitiative!! >= it.second.currentInitiative!! }
        }
        Assertions.assertIterableEquals(
            listOf("Alpha", "Gamma", "Beta", "Jerry", "Tuffy"),
            combatantsSorted.map { it.name }
        )
    }

    @Test
    fun `test`() {
        Mockito.`when`(random.nextInt(1, 20))
            .thenReturn(17, 14, 10, 10, 15, 7)
        val initiative = Initiative(sorter = sorter, random = random)
        initiative.executeCommands(
            defaultName = "Fallback",
            "start",
            "add +4 Tom",
            "add +3 Jerry",
            "roll -5 Tom",
            "roll +2 Jerry",
            "round",
            "next",
            "next",
        )

        Assertions.assertTrue {
            initiative.execute("next", "Fallback") is Failure
        }

        initiative.executeCommands(
            defaultName = "Fallback",
            "end-round",
            "roll -5 Tom",
        )

        Assertions.assertTrue {
            initiative.execute("round", "Fallback") is Failure
        }

        initiative.executeCommands(
            defaultName = "Fallback",
            "roll +2 Jerry",
            "roll -3 Tom",
            "round",
        )
    }

    @Test
    fun `should fail if end is requested before start`() {
        Assertions.assertThrows(IllegalStateException::class.java) {
            val initiative = Initiative(sorter = sorter, random = random)
            val defaultName = "Fallback"
            initiative.executeCommands(defaultName, "end")
        }
    }

    @Test
    fun `should fail if start is called multiple times`() {
        Assertions.assertThrows(IllegalStateException::class.java) {
            val initiative = Initiative(sorter = sorter, random = random)
            val defaultName = "Fallback"
            initiative.executeCommands(defaultName, "start", "start")
        }
    }
}

private fun Initiative.executeCommands(defaultName: String, vararg commands: String) =
    commands.forEach {
        val result = execute(it, defaultName)
        if (result is Failure) {
            throw result.t
        }
    }
