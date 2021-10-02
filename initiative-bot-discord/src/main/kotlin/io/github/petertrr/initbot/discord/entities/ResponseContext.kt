package io.github.petertrr.initbot.discord.entities

import discord4j.core.`object`.entity.User
import io.github.petertrr.initbot.Initiative

/**
 * @property author the user who initiated message, to which we are currently responding
 */
class ResponseContext(
    val author: User,
    val channelState: ChannelState,
    private val initiative: Initiative,
) {
    val authorState: UserState
        get() = channelState.userStates.computeIfAbsent(author) { UserState() }

    fun isLastCombatant() = !initiative.hasNextCombatant()
}