package org.jetbrains.squash.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

/**
 * Interface for logging.
 * *
 * @author Vitaliy Yasenenko.
 */
interface SquashLoggable {

    val log: Logger
        get() = LoggerFactory.getLogger(this::class.java)

    fun <T> T.log(level: Level = Level.DEBUG, function: T.() -> String): T {
        function().log(level)
        return this
    }

    fun String.log(level: Level = Level.DEBUG): String {
        val text = this
        log.apply {
            when (level) {
                Level.INFO -> info(text)
                Level.DEBUG -> debug(text)
                Level.TRACE -> trace(text)
                Level.WARN -> warn(text)
                Level.ERROR -> error(text)
            }
        }
        return this
    }
}