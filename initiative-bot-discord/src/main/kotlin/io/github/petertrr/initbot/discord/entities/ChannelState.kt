package io.github.petertrr.initbot.discord.entities

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User

data class ChannelState(
    val channelId: Snowflake,
    val userStates: MutableMap<User, UserState> = mutableMapOf(),
)
