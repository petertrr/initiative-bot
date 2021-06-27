package io.github.petertrr.initbot.discord

import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
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
import reactor.core.publisher.Mono
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
        val initiative = initiatives.computeIfAbsent(message.channelId.asString()) {
            logger.info("Creating new Initiative for channel $it")
            Initiative()
        }
        logger.info("Received message `${message.content}`, will run command `$rawCommand`")

        val result = try {
            initiative.execute(rawCommand, author.get().username)
        } catch (e: Exception) {
            logger.error("Error executing command", e)
            Failure(e)
        }

        val response: String = when (result) {
            is Success -> result.message
            is RollResult -> {
                logger.info("@${author.get().username} rolled ${result.roll}")
                "@${author.get().username} rolled `${result.roll} + (${result.modifier}) = ${result.total}` for character ${result.name}"
            }
            is Failure -> "Error during command `$rawCommand`: `${result.t.javaClass.simpleName}: ${result.t.message}`"
            is CountdownStarted -> "${result.combatant.name} (init ${result.combatant.currentInitiative}), you have ${result.period} seconds for your turn!".also {
                countdownSubscription = message.channel.startCountdown(author.get(), result.period).subscribe()
            }
            is RoundResult -> formatRoundMessage(result)
        }

        message.channel.flatMap { messageChannel ->
            messageChannel.createMessage {
                it.setAllowedMentions(AllowedMentions.builder().allowUser(author.get().id).build())
                it.setContent(response)
            }
        }
            .subscribe()
    }

    private fun Mono<MessageChannel>.startCountdown(author: User, seconds: Int): Flux<Message> {
        if (::countdownSubscription.isInitialized && !countdownSubscription.isDisposed) {
            logger.info("Canceling the previous countdown")
            countdownSubscription.dispose()
        }
        return this.flatMapMany { messageChannel ->
            val numIntervals = seconds / COUNTDOWN_INTERVAL_SECONDS.toInt()
            Flux.interval(Duration.ofSeconds(COUNTDOWN_INTERVAL_SECONDS))
                .delaySubscription(Duration.ofSeconds(COUNTDOWN_INTERVAL_SECONDS))
                .take(numIntervals.toLong())
                .flatMap { i ->
                    messageChannel.createMessage {
                        it.setAllowedMentions(AllowedMentions.builder().allowUser(author.id).build())
                        if (i + 1 < numIntervals) {
                            it.setContent("${(numIntervals - i - 1) * COUNTDOWN_INTERVAL_SECONDS} seconds left!")
                        } else {
                            it.setContent("Time is up!")
                        }
                    }
                }
        }
            .log()
    }

    private fun formatRoundMessage(roundResult: RoundResult): String {
        val longestLineLength = roundResult.combatants.map {
            it.name.length + it.currentInitiative!!.toString().length
        }
            .maxOrNull()!!
        return "Starting a new round of initiative:" + roundResult.combatants.joinToString(System.lineSeparator(), prefix = System.lineSeparator()) {
            it.name +  it.currentInitiative.toString().prependIndent(".".repeat(longestLineLength - it.name.length))
        }
    }

    companion object {
        internal const val BOT_PREFIX = "!ib"

        /**
         * An interval to post updates on countdown to the chat
         */
        private const val COUNTDOWN_INTERVAL_SECONDS = 15L
    }
}