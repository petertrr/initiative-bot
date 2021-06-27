package io.github.petertrr.initbot

import io.github.petertrr.initbot.entities.Combatant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.random.Random

@ExtendWith(MockitoExtension::class)
class InitiativeTest {
    @Mock lateinit var random: Random

    @Test
    fun `default initiative flow`() {
        Mockito.`when`(random.nextInt(1, 20))
            .thenReturn(1, 3, 5)
        val initiative = Initiative(random = random)
        val defaultName = "Fallback"

        initiative.executeCommands(
            defaultName = defaultName,
            "start",
            "add 0 Tom",
            "add 0 Jerry",
            "add 0 Tuffy",
        )

        Assertions.assertIterableEquals(
            listOf(Combatant("Tom", 0), Combatant("Jerry", 0), Combatant("Tuffy", 0)),
            initiative.members
        )

        // first round
        initiative.executeCommands(
            defaultName = defaultName,
            "roll 0 Tom",
            "roll +2 Jerry",
            "roll 2 Tuffy",
        )

        Assertions.assertIterableEquals(
            listOf(
                Combatant("Tuffy", 0, 7),
                Combatant("Jerry", 0, 5),
                Combatant("Tom", 0, 1),
            ),
            (initiative.round() as RoundResult).combatants.asIterable()
        )

        initiative.executeCommands(
            defaultName = defaultName,
            "next",
            "next",
            "next",
        )

        Assertions.assertTrue(initiative.members.all { it.currentInitiative == null }) {
            "Should have removed old initiatives after last participant's turn"
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

    @Test
    fun `should fail if end is requested before start`() {
        Assertions.assertThrows(IllegalStateException::class.java) {
            val initiative = Initiative(random = random)
            val defaultName = "Fallback"
            initiative.executeCommands(defaultName, "end")
        }
    }

    @Test
    fun `should fail if start is called multiple times`() {
        Assertions.assertThrows(IllegalStateException::class.java) {
            val initiative = Initiative(random = random)
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
