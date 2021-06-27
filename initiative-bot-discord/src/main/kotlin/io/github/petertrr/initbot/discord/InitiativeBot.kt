package io.github.petertrr.initbot.discord

import discord4j.core.DiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.AllowedMentions
import io.github.petertrr.initbot.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toMono
import java.time.Duration

private val logger = KotlinLogging.logger {}
class InitiativeBot {
    private lateinit var client: DiscordClient
    private val initiatives = mutableMapOf<String, Initiative>()
    private lateinit var countdownSubscription: Disposable

    fun start(args: List<String>) {
        client = DiscordClient.create(args.first())

        client.withGateway {
            mono {
                it.on(MessageCreateEvent::class.java)
                    .asFlow()
                    .filter { it.message.content.startsWith(BOT_PREFIX) }
                    .collect { handleMessageCreateEvent(it) }
                    .toMono()
            }
        }
            .block()
    }

    private fun handleMessageCreateEvent(messageCreateEvent: MessageCreateEvent) {
        val message = messageCreateEvent.message
        val author = message.author
        val rawCommand = message.content.dropWhile { it in BOT_PREFIX }.trim()
        // todo: require explicit start instead of default value in map
        val initiative = initiatives.getOrDefault(message.channelId.asString(), Initiative())
        logger.info("Received message `${message.content}`, will run command `$rawCommand`")
        val response = when (val result = initiative.execute(rawCommand)) {
            is Success -> result.message
            is RollResult -> {
                logger.info("@${author.get().username} rolled ${result.roll}")
                "@${author.get().username} rolled `${result.roll} + (${result.modifier}) = ${result.total}`"

            }
            is Failure -> "Error during command `$rawCommand`: `${result.t.message}`"
            is CountdownStarted -> {
                if (::countdownSubscription.isInitialized && !countdownSubscription.isDisposed) {
                    logger.info("Canceling the previous countdown")
                    countdownSubscription.dispose()
                }
                countdownSubscription = message.channel.flatMapMany { messageChannel ->
                    val numIntervals = result.period / COUNTDOWN_INTERVAL_SECONDS.toInt()
                    Flux.interval(Duration.ofSeconds(COUNTDOWN_INTERVAL_SECONDS))
                        .delaySubscription(Duration.ofSeconds(COUNTDOWN_INTERVAL_SECONDS))
                        .take(numIntervals.toLong())
                        .flatMap { i ->
                            messageChannel.createMessage {
                                it.setAllowedMentions(AllowedMentions.builder().allowUser(author.get().id).build())
                                if (i + 1 < numIntervals) {
                                    it.setContent("${(numIntervals - i - 1) * COUNTDOWN_INTERVAL_SECONDS} seconds left!")
                                } else {
                                    it.setContent("Time is up!")
                                }
                            }
                        }
                    }
                    .log()
                    .subscribe()
                "Starting countdown for ${result.period} seconds"
            }
        }
        message.channel.flatMap { messageChannel ->
            messageChannel.createMessage {
                it.setAllowedMentions(AllowedMentions.builder().allowUser(author.get().id).build())
                it.setContent(response)
            }
        }.subscribe()
    }

    companion object {
        internal const val BOT_PREFIX = "!ib"

        /**
         * An interval to post updates on countdown to the chat
         */
        private const val COUNTDOWN_INTERVAL_SECONDS = 15L
    }
}