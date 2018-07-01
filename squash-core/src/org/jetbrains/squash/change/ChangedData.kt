package org.jetbrains.squash.change

import java.time.LocalDateTime

/**
 * Executed change log data class.
 * *
 * @author Vitaliy Yasenenko.
 */
data class ChangedData(val vid: Int, val name: String, val query: String, private val whenChange: LocalDateTime) {
    override fun toString(): String  = "[name '$name': [$vid] ($query) when $whenChange]"
}
