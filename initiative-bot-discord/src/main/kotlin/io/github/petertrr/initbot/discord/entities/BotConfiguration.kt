package io.github.petertrr.initbot.discord.entities

import java.util.*

/**
 * @property turnDurationSeconds An interval to post updates on countdown to the chat
 */
data class BotConfiguration(
    val prefix: String = "!ib",
    val turnDurationSeconds: Long = 15L,
) {
    companion object {
        fun fromProperties(properties: Properties): BotConfiguration = BotConfiguration(
            prefix = properties.getProperty("bot.prefix"),
            turnDurationSeconds = properties.getProperty("turn.duration.seconds").toLong(),
        )
    }
}
