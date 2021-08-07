package io.github.petertrr.initbot.discord

import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.AllowedMentions
import io.github.petertrr.initbot.*
import io.github.petertrr.initbot.discord.entities.BotConfiguration
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
class InitiativeBot(private val botConfiguration: BotConfiguration) {
    private lateinit var client: DiscordClient
    private val initiatives = mutableMapOf<String, Initiative>()
    private val userNameByCharacterNameByChannel = ConcurrentHashMap<String, MutableMap<User, UserState>>()
    private lateinit var countdownSubscription: Disposable

    fun start(args: List<String>) {
        client = DiscordClient.create(args.first())

        client.withGateway { gatewayDiscordClient ->
            gatewayDiscordClient.on(MessageCreateEvent::class.java)
                .filter { it.message.content.startsWith(botConfiguration.prefix) }
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
        val rawCommand = message.content.dropWhile { it in botConfiguration.prefix }.trim()
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
            is RemoveSuccess -> {
                // todo: if user attempts to remove other user's character, they will succeed in initiative-bot-core, but fail here
                val userState = computeUserStateIfAbsent(author, channelId.asString())
                userState.characterNames.removeIf { it == result.name }
                result.message
            }
            is RollResult -> {
                if (::countdownSubscription.isInitialized && !countdownSubscription.isDisposed) {
                    logger.info { "Disposing countdown subscription, because ${author.username} has started rolling for new round" }
                    countdownSubscription.dispose()
                }
                logger.info("@${author.username} rolled ${result.roll}")
                "${author.mention} rolled `${result.roll} + (${result.modifier}) = ${result.total}` for character ${result.name}"
            }
            is Failure -> "Error during command `$rawCommand`: `${result.t.javaClass.simpleName}: ${result.t.message}`"
            is CountdownStarted -> {
                val user = userNameByCharacterNameByChannel[channelId.asString()]!!.filterValues {
                    result.combatant.name in it.characterNames
                }.keys.single()
                "${result.combatant.name} (init ${result.combatant.currentInitiative}) is up, ${user.mention}, you have ${result.period} seconds for your turn!".let {
                    if (initiative.hasNextCombatant()) it else "$it Also, you are the last in this round, everyone should call `roll` after your turn."
                }.also {
                    countdownSubscription = message.channel.startCountdown(author, result.period).subscribe()
                }
            }
            is RoundResult -> formatRoundMessage(result, channelId.asString())
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
            val numIntervals = seconds / botConfiguration.turnDurationSeconds
            Flux.interval(Duration.ofSeconds(botConfiguration.turnDurationSeconds))
                .delaySubscription(Duration.ofSeconds(botConfiguration.turnDurationSeconds))
                .take(numIntervals)
                .flatMap { i ->
                    messageChannel.createMessage {
                        it.setAllowedMentions(AllowedMentions.builder().allowUser(author.id).build())
                        if (i + 1 < numIntervals) {
                            it.setContent("${(numIntervals - i - 1) * botConfiguration.turnDurationSeconds} seconds left!")
                        } else {
                            it.setContent("Time is up!")
                        }
                    }
                }
        }
            .log()
    }

    private fun formatRoundMessage(roundResult: RoundResult, channelId: String): String {
        val longestLineLength = roundResult.combatants.maxOf {
            it.name.length + it.currentInitiative!!.toString().length
        }
        return "Starting a new round of initiative:" +
                roundResult.combatants.joinToString(System.lineSeparator(), prefix = System.lineSeparator()) { combatant ->
                    val owner = userNameByCharacterNameByChannel[channelId]!!.filterValues {
                        combatant.name in it.characterNames
                    }.keys.single()
                    combatant.name + ".".repeat(longestLineLength - combatant.name.length) + combatant.currentInitiative +
                            " ... owner ${owner.mention}"
                }
    }
}