package org.jetbrains.squash.change

import org.jetbrains.squash.dialect.SQLStatement
import org.jetbrains.squash.dialect.SQLStatementBuilder
import org.jetbrains.squash.util.SquashLoggable
import java.util.concurrent.atomic.AtomicInteger

/**
 * Abstract change log statement for custom query.
 *
 *    For example:
 *
 *    object ExampleChangeLog : ChangeLogStatement(name = "your_custom_name", changing = {
 *
 *        // this - auto numeration
 *        this[ "YOUR CUSTOM" ]
 *        this[ "QUERIES" ]
 *
 *        // or manual numerating
 *        1[ "YOUR CUSTOM" ]
 *        2[ "QUERIES" ]
 *    })
 *
 * @author Vitaliy Yasenenko.
 */
abstract class ChangeLogStatement(private val name: String? = null, changing: ChangeLogStatement.() -> Unit) : SquashLoggable {

    private val count: AtomicInteger = AtomicInteger(0)

    private val numbers = mutableListOf<Int>()

    val list: MutableList<ChangingExecutedStatement> = mutableListOf()

    val nameChangeLog: String
        get() = name ?: this::class.simpleName ?: "ChangeLogStatement"

    init {
        changing()
    }

    private fun getNumber(): Int {
        val incrementAndGet = count.incrementAndGet()
        return if (numbers.contains(incrementAndGet)) getNumber() else incrementAndGet
    }

    operator fun Int.get(query: String) {
        check(count.get() < this) { "This [$this] number lower than auto incremented counting" }
        check(!numbers.contains(this)) { "This [$this] number already exist" }
        numbers.add(this)
        list.add(ChangingExecutedStatement(this, query))
    }

    operator fun get(query: String) {
        list.add(ChangingExecutedStatement(getNumber(), query))
    }

    operator fun get(statement: SQLStatement) = get(statement.sql)

    operator fun get(statement: SQLStatementBuilder) = get(statement.build())

    data class ChangingExecutedStatement(val vid: Int, val query: String) {
        override fun toString(): String = "[$vid] ($query)"
    }
}