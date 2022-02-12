package io.github.petertrr.initbot.discord.entities

import java.util.*

/**
 * @property turnUpdateSeconds An interval to post updates on countdown to the chat
 * @property countdownCompat if true, than turn duration countdown will be performed for other commands, even if no
 * initiative tracking is currently active in initiative-bot
 */
data class BotConfiguration(
    val prefix: String = "!ib",
    val turnDurationSeconds: Long = 45L,
    val turnUpdateSeconds: Long = 15L,
    val countdownCompat: Boolean = false,
) {
    companion object {
        fun fromProperties(properties: Properties): BotConfiguration = BotConfiguration(
            prefix = properties.getProperty("bot.prefix"),
            turnDurationSeconds = properties.getProperty("turn.duration.seconds").toLong(),
            turnUpdateSeconds = properties.getProperty("turn.update.seconds").toLong(),
            countdownCompat = properties.getProperty("bot.countdown.compat").toBoolean(),
        )
    }
}
