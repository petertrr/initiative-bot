package io.github.petertrr.initbot.discord

import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.AllowedMentions
import io.github.petertrr.initbot.*
import io.github.petertrr.initbot.discord.entities.UserState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}
class InitiativeBot {
    private lateinit var client: DiscordClient
    private val initiatives = mutableMapOf<String, Initiative>()
    private val userNameByCharacterNameByChannel = ConcurrentHashMap<String, MutableMap<User, UserState>>()
    private lateinit var countdownSubscription: Disposable

    fun start(args: List<String>) {
        client = DiscordClient.create(args.first())

        client.withGateway { gatewayDiscordClient ->
            gatewayDiscordClient.on(MessageCreateEvent::class.java)
                .filter { it.message.content.startsWith(BOT_PREFIX) }
                .flatMap {
                    initializeInitiativeIfAbsent(it.message.channelId.asString())
                    handleMessageCreateEvent(it)
                }
        }
            .block()
    }

    private fun initializeInitiativeIfAbsent(channelId: String) {
        initiatives.computeIfAbsent(channelId) {
            logger.info("Creating new Initiative for channel $it")
            Initiative()
        }
        userNameByCharacterNameByChannel.computeIfAbsent(channelId) {
            mutableMapOf()
        }
    }

    private fun getCharacterNameFor(user: User, channelId: String): String {
        val characterNames = userNameByCharacterNameByChannel[channelId]!![user]
            ?.characterNames
        return characterNames
            ?.singleOrNull()
            ?: user.username.also {
                logger.warn {
                    "User $it has ${if (characterNames == null) "no" else "multiple ($characterNames)"} added characters, so will use username as fallback"
                }
            }
    }

    private fun computeUserStateIfAbsent(user: User, channelId: String): UserState {
        return userNameByCharacterNameByChannel[channelId]!!.computeIfAbsent(user) {
            UserState()
        }
    }

    private fun handleMessageCreateEvent(messageCreateEvent: MessageCreateEvent): Mono<Message> {
        val message = messageCreateEvent.message
        val channelId = message.channelId
        val author = message.author.get()
        val rawCommand = message.content.dropWhile { it in BOT_PREFIX }.trim()
        val initiative = initiatives[channelId.asString()]!!
        logger.info("Received message `${message.content}`, will run command `$rawCommand`")

        val result = try {
            val fallbackName = getCharacterNameFor(author, channelId.asString())
            initiative.execute(rawCommand, fallbackName)
        } catch (e: Exception) {
            logger.error("Error executing command", e)
            Failure(e)
        }

        val response: String = when (result) {
            is Success -> result.message
            is AddSuccess -> {
                val userState = computeUserStateIfAbsent(author, channelId.asString())
                if (result.name !in userState.characterNames) {
                    userState.characterNames.add(result.name)
                }
                result.message
            }
            is RollResult -> {
                logger.info("@${author.username} rolled ${result.roll}")
                "@${author.username} rolled `${result.roll} + (${result.modifier}) = ${result.total}` for character ${result.name}"
            }
            is Failure -> "Error during command `$rawCommand`: `${result.t.javaClass.simpleName}: ${result.t.message}`"
            is CountdownStarted -> "${result.combatant.name} (init ${result.combatant.currentInitiative}), you have ${result.period} seconds for your turn!".let {
                if (initiative.hasNextCombatant()) it else "$it Also, you are the last in this round, everyone should call `roll` after your turn."
            }.also {
                countdownSubscription = message.channel.startCountdown(author, result.period).subscribe()
            }
            is RoundResult -> formatRoundMessage(result)
        }

        return message.channel.flatMap { messageChannel ->
            messageChannel.createMessage {
                it.setAllowedMentions(AllowedMentions.builder().allowUser(author.id).build())
                it.setContent(response)
            }
        }
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