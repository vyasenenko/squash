package org.jetbrains.squash.tests

import org.jetbrains.squash.connection.Transaction
import org.jetbrains.squash.connection.invoke
import org.jetbrains.squash.definition.Table

fun <T : Table> Transaction.exists(table: T): Boolean {
    return databaseSchema().tables().any { String.CASE_INSENSITIVE_ORDER.compare(it.name, table.compoundName.id) == 0 }
}

class CheckQuery(transaction: Transaction) {

    private var _count: Int = 0

    val countQueries: Int
        get() = _count

    init {
        transaction.connection.monitor {
            before {
                _count++
            }
        }
    }
}