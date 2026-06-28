package com.tsourcecode.wiki.lib.domain.util


interface Logger {
    fun fork(prefix: String): Logger
    fun log(message: () -> String)

    companion object {
        operator fun invoke(
            prefix: String = "",
            performer: (String) -> Unit,
            ): Logger {
            return LoggerImpl(prefix, performer)
        }
    }
}

class CompositeLogger(
    vararg loggers: Logger,
): Logger {
    private val loggers: List<Logger> = loggers.toList()

    override fun fork(prefix: String): Logger {
        return CompositeLogger(
            *loggers
                .map { it.fork(prefix) }
                .toTypedArray())
    }

    override fun log(message: () -> String) {
        loggers.forEach { it.log(message) }
    }
}

private class LoggerImpl(
    val prefix: String = "",
    val performer: (String) -> Unit,
): Logger {
    override fun fork(prefix: String): Logger {
        return LoggerImpl(
            this.prefix + prefix,
            performer,
        )
    }

    override fun log(message: () -> String) {
        emit(message.invoke())
    }

    private fun emit(msg: String) {
        performer(prefix + msg)
    }
}
