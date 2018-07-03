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

class CheckQuery(transaction: Transaction) {

    private val list: MutableList<SQLStatement> = mutableListOf()

    private var nameQueries: List<Map<Int, String>> = emptyList()

    private var _count: Int = 0

    val countQueries: Int
        get() = _count

    init {
        transaction.connection.monitor {
            before {
                _count++
                list += it
            }
        }
    }

    fun assert(expectedCount: Int, vararg nameQueries: String) {
        if (nameQueries.isEmpty() && this.nameQueries.isEmpty()) {
            assertEquals(expectedCount, countQueries, "Expected count query - $expectedCount" +
                    "\nAll queries:\n\t${list.joinToString("\n\t")}")
            return
            } else if (nameQueries.isNotEmpty()) {
                    this.nameQueries += nameQueries.map { string ->
                val endIndex = string.indexOfFirst { it == ']' }
                string.substring(string.indexOfFirst { it == '[' } + 1, endIndex).split(",")
                        .mapNotNull { it.trim().toIntOrNull()?.let { it - 1 }?.let { it to string.substring(endIndex + 1) } }.toMap()
            }
        }

        assertEquals(expectedCount, countQueries,
                "Expected count query - $expectedCount\nAll queries:\n\t${this.list.mapIndexed { index, sqlStatement ->
                    this.nameQueries.find { it[index] != null }?.let { "[${index + 1}]${it[index]} : ${sqlStatement.sql}" }
                            ?: sqlStatement.sql
                }.joinToString("\n\t")}")
    }
}