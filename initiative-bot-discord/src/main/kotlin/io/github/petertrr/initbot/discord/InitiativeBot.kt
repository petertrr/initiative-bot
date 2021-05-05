package io.github.petertrr.initbot.discord

import discord4j.core.DiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.AllowedMentions
import io.github.petertrr.initbot.Initiative
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.kotlin.core.publisher.toMono

private val logger = KotlinLogging.logger {}
class InitiativeBot {
    private lateinit var client: DiscordClient
    private val initiatives = mutableMapOf<String, Initiative>()

    fun start(args: List<String>) {
        client = DiscordClient.create(args.first())

        client.withGateway {
            mono {
                it.on(MessageCreateEvent::class.java)
                    .asFlow()
                    .filter {
                        it.message.content.startsWith(BOT_PREFIX)
                    }
                    .collect {
                        val message = it.message
                        val author = message.author
                        val command = message.content.dropWhile { it in BOT_PREFIX }.trim()
                        val initiative = initiatives[message.channelId.asString()]
                        logger.info("Received message `${message.content}`, will run command `$command`")
                        when {
                            command == "start" -> initiatives[message.channelId.asString()] = Initiative()
                            command.startsWith("roll") -> {
                                val response = if (initiative == null) {
                                    "Initiative not started, run `$BOT_PREFIX start` first"
                                } else {
                                    val roll = initiative.roll("test", 3)
                                    logger.info("@${author.get().username} rolled $roll")
                                    "@${author.get().username} rolled `$roll`"
                                }
                                message.channel.flatMap {
                                    it.createMessage {
                                        it.setAllowedMentions(AllowedMentions.builder().allowUser(author.get().id).build())
                                        it.setEmbed {
                                        }
                                        it.setContent(response)
                                    }
                                }.subscribe()
                            }
                            else -> message.channel.flatMap {
                                it.createMessage("Unknown command `$command`")
                            }.subscribe()
                        }
                    }
                    .toMono()
            }
        }
            .block()
    }

    companion object {
        internal const val BOT_PREFIX = "!ib"
    }
}