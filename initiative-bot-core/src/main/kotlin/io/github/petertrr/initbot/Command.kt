package io.github.petertrr.initbot

import io.github.petertrr.initbot.entities.InitiativeMode
import java.lang.IllegalArgumentException

sealed class Command(command: String) {
    companion object {
        fun parse(rawCommand: String, defaultName: String = "Fallback", defaultRoundSeconds: Long): Command {
            val parts = rawCommand.split(" ")
            return when (parts.first().lowercase()) {
                "start" -> Start(parts.getOrNull(1)?.let(InitiativeMode::valueOf) ?: InitiativeMode.REGULAR)
                "end" -> End
                "add" -> Add(parts.getOrElse(2) { defaultName }, parts[1].toInt())
                "remove" -> Remove(parts.getOrElse(1) { defaultName })
                "round" -> Round
                "end-round" -> EndRound
                "roll" -> Roll(parts.getOrElse(2) { defaultName }, parts[1].toInt())
                "next" -> Countdown(defaultRoundSeconds)
                "help" -> Help
                else -> throw IllegalArgumentException("Malformed roll command: `$rawCommand`")
            }
        }
    }
}

object Help : Command("help")

data class Start(val mode: InitiativeMode) : Command("start")

object End : Command("end")

object Round : Command("round")

object EndRound : Command("end-round")

data class Roll(val name: String, val modifier: Int) : Command("roll") {
    companion object {
        internal val cmdRegex = Regex("roll ([+\\-\\d]+)([\\s\\w]*)")
    }
}

data class Add(val name: String, val baseModifier: Int) : Command("add")

data class Remove(val name: String) : Command("remove")

data class Countdown(val seconds: Long) : Command("countdown")