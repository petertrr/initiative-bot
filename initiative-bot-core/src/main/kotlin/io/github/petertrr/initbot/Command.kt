package io.github.petertrr.initbot

import java.lang.IllegalArgumentException

sealed class Command(val command: String) {
    companion object {
        fun parse(rawCommand: String): Command {
            return when {
                rawCommand.equals("start", ignoreCase = true) -> Start
                rawCommand.equals("end", ignoreCase = true) -> End
//                rawCommand.equals("round", ignoreCase = true) -> Round
                rawCommand.startsWith("roll", ignoreCase = true) -> {
                    Roll.cmdRegex.find(rawCommand)?.groups?.let {
                        Roll(it[2]!!.value, it[1]!!.value.toInt())
                    } ?: throw IllegalArgumentException("Malformed roll command, should be `roll <modifier> [name]`")
                }
                rawCommand.startsWith("remove", ignoreCase = true) -> Remove(rawCommand.substringAfter("remove").trim())
                // todo: configurable countdown interval
                rawCommand.equals("next", ignoreCase = true) -> Countdown(60)
                else -> throw IllegalArgumentException("Malformed roll command: `$rawCommand`")
            }
        }
    }
}

object Start : Command("start")

object End : Command("end")

//object Round : Command("round")

class Roll(val name: String, val modifier: Int) : Command("roll") {
    companion object {
        internal val cmdRegex = Regex("roll ([+\\-\\d]+)([\\s\\w]*)")
    }
}

class Remove(val name: String) : Command("remove")

class Countdown(val seconds: Int) : Command("countdown")