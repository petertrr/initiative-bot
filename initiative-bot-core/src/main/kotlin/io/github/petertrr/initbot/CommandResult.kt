package io.github.petertrr.initbot

sealed class CommandResult

data class Success(val message: String) : CommandResult()

data class RollResult(val roll: Int, val modifier: Int) : CommandResult() {
    val total = roll + modifier
}

data class Failure(val t: Throwable) : CommandResult()

data class CountdownStarted(val period: Int) : CommandResult()
