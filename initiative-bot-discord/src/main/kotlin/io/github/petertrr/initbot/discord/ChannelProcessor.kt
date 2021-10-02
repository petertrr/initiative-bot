package io.github.petertrr.initbot.discord

import io.github.petertrr.initbot.Initiative
import io.github.petertrr.initbot.discord.entities.ChannelState
import reactor.core.Disposable

class ChannelProcessor(
    internal val initiative: Initiative,
    internal val responseProcessor: ResponseProcessor,
    val channelState: ChannelState
) {
    private lateinit var countdownSubscription: Disposable
}