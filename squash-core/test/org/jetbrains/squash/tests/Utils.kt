package org.jetbrains.squash.tests

import org.jetbrains.squash.connection.Transaction
import org.jetbrains.squash.connection.invoke
import org.jetbrains.squash.definition.Table
import org.jetbrains.squash.dialect.SQLStatement
import org.jetbrains.squash.util.Loggable
import kotlin.test.assertEquals

fun <T : Table> Transaction.exists(table: T): Boolean {
    return databaseSchema().tables().any { String.CASE_INSENSITIVE_ORDER.compare(it.name, table.compoundName.id) == 0 }
}

class CheckQuery(transaction: Transaction): Loggable {

    private val _queries: MutableList<SQLStatement> = mutableListOf()

    private val _nameQueries: MutableList<Map<Int, String>> = mutableListOf()

    private var _count: Int = 0

    val countQueries: Int
        get() = _count

    init {
        transaction.connection.monitor {
            before {
                _count++
                _queries += it
            }
        }
    }

    /**
     * Asserts that the [expectedCount] value is equal to the [countQueries] value, with an optional [nameQueries].
     * for example:
     *      .assert(expectedCount = 2, nameQueries = "[1] select query", "[2] insert query")
     */
    fun assert(expectedCount: Int, vararg nameQueries: String) {
        _nameQueries.addAll(nameQueries.map { string ->
            val endIndex = string.indexOfFirst { it == ']' }
            string.substring(string.indexOfFirst { it == '[' } + 1, endIndex).split(",")
                    .mapNotNull { it.trim().toIntOrNull()?.let { it - 1 }?.let { it to string.substring(endIndex + 1).trim() } }.toMap()
        })

        assertEquals(expectedCount, _count, "Expected count query - $expectedCount" +
                "\nAll queries:\n\t${_queries.mapIndexed { i, sql ->
                    _nameQueries.find { it[i] != null }?.let { "[${i + 1}] ${it[i]} : ${sql.sql}" } ?: sql.sql
                }.joinToString("\n\t")}".log())
    }
}