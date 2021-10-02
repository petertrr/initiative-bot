package io.github.petertrr.initbot.discord

import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.AllowedMentions
import io.github.petertrr.initbot.*
import io.github.petertrr.initbot.discord.entities.BotConfiguration
import io.github.petertrr.initbot.discord.entities.ChannelState
import io.github.petertrr.initbot.discord.entities.ResponseContext
import io.github.petertrr.initbot.sorting.DescendantSorter
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}
class InitiativeBot(private val botConfiguration: BotConfiguration) {
    private lateinit var client: DiscordClient
    private val initiatives = ConcurrentHashMap<Snowflake, Initiative>()
    private val responseProcessors = ConcurrentHashMap<Snowflake, ResponseProcessor>()
    private val channelStates = ConcurrentHashMap<Snowflake, ChannelState>()
    private lateinit var countdownSubscription: Disposable

    fun start(args: List<String>) {
        client = DiscordClient.create(args.first())

        client.withGateway { gatewayDiscordClient ->
            gatewayDiscordClient.on(MessageCreateEvent::class.java)
                .filter { it.message.content.startsWith(botConfiguration.prefix) }
                .flatMap {
                    initializeInitiativeIfAbsent(it.message.channelId)
                    handleMessageCreateEvent(it)
                }
        }
            .block()
    }

    private fun initializeInitiativeIfAbsent(channelId: Snowflake) {
        initiatives.computeIfAbsent(channelId) {
            logger.info { "Creating new Initiative for channel $it" }
            Initiative(
                sorter = DescendantSorter(),
                turnDurationSeconds = botConfiguration.turnDurationSeconds
            )
        }
        channelStates.computeIfAbsent(channelId) {
            ChannelState(channelId)
        }
        responseProcessors.computeIfAbsent(channelId) {
            ResponseProcessor.create {
                doOn(AddSuccess::class) {
                    if (it.name !in authorState.characterNames) {
                        authorState.characterNames.add(it.name)
                    }
                }
                doOn(RemoveSuccess::class) { commandResult ->
                    // todo: if user attempts to remove other user's character, they will succeed in initiative-bot-core, but fail here
                    authorState.characterNames.removeIf { it == commandResult.name }
                }
                doOn(EndSuccess::class) {
                    cleanup(channelId)
                }
                mapIf(RollResult::class) {
                    logger.info("@${author.username} rolled ${it.roll}")
                    "${author.mention} rolled `${it.roll} + (${it.modifier}) = ${it.total}` for character ${it.name}"
                }
                mapIf(RoundResult::class) { formatRoundMessage(it, channelId) }
                mapIf(CountdownStarted::class) { commandResult ->
                    val user = channelState.userStates.filterValues {
                        commandResult.combatant.name in it.characterNames
                    }.keys.single()
                    "${commandResult.combatant.name} (init ${commandResult.combatant.currentInitiative}) is up, ${user.mention}, you have ${commandResult.period} seconds for your turn!".let {
                        if (isLastCombatant()) "$it Also, you are the last in this round, DM should call `end-round` after." else it
                    }
                }
            }
        }
    }

    private fun handleMessageCreateEvent(messageCreateEvent: MessageCreateEvent): Mono<Message> {
        val message = messageCreateEvent.message
        val channelId = message.channelId
        val author = message.author.get()
        val rawCommand = message.content.dropWhile { it in botConfiguration.prefix }.trim()
        val initiative = initiatives[channelId]!!
        val channelState = channelStates[channelId]!!
        val responseProcessor = responseProcessors[channelId]!!
        logger.info { "Received message `${message.content}` from `${author.username}`, will run command `$rawCommand`" }

        val result = try {
            val fallbackName = getCharacterNameFor(author, channelId)
            initiative.execute(rawCommand, fallbackName)
        } catch (e: Exception) {
            logger.error("Error executing command", e)
            Failure(e, rawCommand)
        }

        val response = responseProcessor.process(
            ResponseContext(author, channelState, initiative),
            result
        )

        if (result is CountdownStarted) {
            if (::countdownSubscription.isInitialized && !countdownSubscription.isDisposed) {
                logger.info { "Disposing countdown subscription, because it's the next combatant's turn" }
                countdownSubscription.dispose()
            }
            countdownSubscription = message.channel.startCountdown(result.period).subscribe()
        }

        return message.channel.flatMap { messageChannel ->
            messageChannel.createMessage {
                it.setAllowedMentions(AllowedMentions.builder().allowUser(author.id).build())
                it.setContent(response)
            }
        }
    }

    private fun getCharacterNameFor(user: User, channelId: Snowflake): String {
        val characterNames = channelStates[channelId]!!.userStates[user]
            ?.characterNames
        return characterNames
            ?.singleOrNull()
            ?: user.username.also {
                logger.warn {
                    "User $it has ${if (characterNames == null) "no" else "multiple ($characterNames)"} added characters, so will use username as fallback"
                }
            }
    }

    private fun Mono<MessageChannel>.startCountdown(seconds: Long): Flux<Message> {
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

    private fun formatRoundMessage(roundResult: RoundResult, channelId: Snowflake): String {
        val longestLineLength = roundResult.combatants.maxOf {
            it.name.length + it.currentInitiative!!.toString().length
        }
        return "Starting **round ${roundResult.roundIdx}** of initiative:" +
                roundResult.combatants.joinToString(System.lineSeparator(), prefix = System.lineSeparator()) { combatant ->
                    val owner = channelStates[channelId]!!.userStates.filterValues {
                        combatant.name in it.characterNames
                    }.keys.single()
                    combatant.name + ".".repeat(longestLineLength - combatant.name.length) + combatant.currentInitiative +
                            " ... owner ${owner.mention}"
                }
    }

    private fun cleanup(channelId: Snowflake) {
        logger.info { "Removing initiative for channel $channelId" }
        initiatives.remove(channelId)
        logger.info { "Clearing users' character names for channel $channelId" }
        channelStates[channelId]!!.userStates.clear()
    }
}