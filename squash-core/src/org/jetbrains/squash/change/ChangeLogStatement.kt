package org.jetbrains.squash.change

import org.jetbrains.squash.definition.TableDefinition
import org.jetbrains.squash.dialect.SQLStatement
import org.jetbrains.squash.dialect.SQLStatementBuilder
import org.jetbrains.squash.util.Loggable
import java.util.concurrent.atomic.AtomicInteger

/**
 * Abstract change log statement for custom query.
 *
 *    For example:
 *
 *    object ExampleChangeLog : ChangeLogStatement(name = "your_custom_name", changing = {
 *        this[ "YOUR CUSTOM" ]
 *        this[ "QUERIES" ]
 *    })
 *
 * @author Vitaliy Yasenenko.
 */
abstract class ChangeLogStatement(private val name: String? = null, changing: ChangeLogStatement.() -> Unit) : Loggable {

    private val count: AtomicInteger = AtomicInteger(0)

    val list: MutableList<ChangingExecutedStatement> = mutableListOf()

    val nameChangeLog: String
        get() = name ?: this::class.simpleName ?: "ChangeLogStatement"

    init {
        changing()
    }

    operator fun get(query: String) {
        list.add(ChangingExecutedStatement(count.incrementAndGet(), query))
    }

    operator fun get(statement: SQLStatement) = get(statement.sql)

    operator fun get(statement: SQLStatementBuilder) = get(statement.build())

    data class ChangingExecutedStatement(val vid: Int, val query: String) {
        override fun toString(): String = "[$vid] ($query)"
    }
}