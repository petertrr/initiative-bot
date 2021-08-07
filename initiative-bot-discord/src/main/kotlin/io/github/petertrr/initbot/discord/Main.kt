package io.github.petertrr.initbot.discord

import io.github.petertrr.initbot.discord.entities.BotConfiguration
import mu.KotlinLogging
import java.util.*
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    if (args.isEmpty()) {
        logger.error { "Incorrect usage: use `initiative-bot-discord <token>`" }
        exitProcess(-1)
    }
    val properties = Properties().apply {
        load(ClassLoader.getSystemResourceAsStream("application.properties"))
    }
    val configuration = BotConfiguration.fromProperties(properties)
    println(configuration)
    InitiativeBot(configuration).start(args.toList())
}
