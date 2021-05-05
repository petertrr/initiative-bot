package io.github.petertrr.initbot.discord

import mu.KotlinLogging
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    if (args.isEmpty()) {
        logger.error { "Incorrect usage: use `initiative-bot-discord <token>`" }
        exitProcess(-1)
    }
    InitiativeBot().start(args.toList())
}
