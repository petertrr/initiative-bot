package io.github.petertrr.initbot.discord

import io.github.petertrr.initbot.CommandResult
import io.github.petertrr.initbot.discord.entities.ResponseContext
import kotlin.reflect.KClass

class ResponseProcessor private constructor(
    private val handlers: List<ResponseContext.(CommandResult) -> Unit>,
    private val mappers: List<ResponseContext.(CommandResult) -> String?>,
) {
    fun <T : CommandResult> process(ctx: ResponseContext, rawResult: T): String {
        handlers.forEach { handler -> handler(ctx, rawResult) }
        return mappers.firstNotNullOfOrNull { mapper -> mapper(ctx, rawResult) }
            ?: rawResult.message
    }

    class Builder {
        private val handlers: MutableList<ResponseContext.(CommandResult) -> Unit> = mutableListOf()
        private val mappers: MutableList<ResponseContext.(CommandResult) -> String?> = mutableListOf()

        fun build() = ResponseProcessor(handlers, mappers)

        fun <T : CommandResult> doOn(responseType: KClass<T>, handler: ResponseContext.(commandResult: T) -> Unit) {
            handlers.add {
                if (responseType.isInstance(it)) {
                    handler(this, it as T)
                }
            }
        }

        fun <T : CommandResult> mapIf(responseType: KClass<T>, mapper: ResponseContext.(T) -> String) {
            mappers.add {
                if (responseType.isInstance(it)) mapper(this, it as T) else null
            }
        }
    }

    companion object {
        fun create(configure: Builder.() -> Unit) = Builder().apply(configure).build()
    }
}
