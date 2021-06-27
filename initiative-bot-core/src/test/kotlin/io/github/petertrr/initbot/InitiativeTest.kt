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
@OptIn(ExperimentalStdlibApi::class)
class InitiativeTest {
    @Mock lateinit var random: Random

    @Test
    fun `should add combatants and sort according to modifiers`() {
        Mockito.`when`(random.nextInt(1, 20))
            .thenReturn(1, 3, 5)
        val initiative = Initiative(random = random)

        initiative.add(Add("Tom", 0))
        initiative.add(Add("Jerry", 0))
        initiative.add(Add("Tuffy", 0))

        Assertions.assertIterableEquals(
            listOf(Combatant("Tom", 0), Combatant("Jerry", 0), Combatant("Tuffy", 0)),
            initiative.members
        )

        initiative.roll(Roll("Tom", 0))
        initiative.roll(Roll("Jerry", 2))
        initiative.roll(Roll("Tuffy", 2))

        Assertions.assertIterableEquals(
            listOf("Tom", "Jerry", "Tuffy"),
            (initiative.round() as RoundResult).combatants.asIterable()
        )
    }
}
