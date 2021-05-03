package io.github.petertrr.initbot

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
        Mockito.`when`(random.nextInt(1, 20)).thenReturn(1)
        Mockito.`when`(random.nextInt(1, 20)).thenReturn(3)
        Mockito.`when`(random.nextInt(1, 20)).thenReturn(5)
        val initiative = Initiative(random = random)

        initiative.roll("Tom", 0)
        initiative.roll("Jerry", 2)
        initiative.roll("Tuffy", 2)

        Assertions.assertIterableEquals(
            initiative.round().asIterable(),
            listOf("Tom", "Jerry", "Tuffy")
        )
    }
}
