package io.github.petertrr.initbot

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CommandParserTest {
    @Test
    fun `should parse different commands`() {
        val defaultName = "Tester"
        mapOf(
            "help" to Help,
            "start" to Start,
            "end" to End,
            "add +2" to Add(defaultName, 2),
            "add +2 Tom" to Add("Tom", 2),
            "roll +2" to Roll(defaultName, 2),
            "roll +2 Tom" to Roll("Tom", 2),
        ).forEach { (rawCommand, expectedCommand) ->
            Assertions.assertEquals(expectedCommand, Command.parse(rawCommand, defaultName))
        }
    }
}
