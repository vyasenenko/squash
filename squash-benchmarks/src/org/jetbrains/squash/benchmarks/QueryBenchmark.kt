package org.jetbrains.squash.benchmarks

import kotlinx.coroutines.experimental.*
import org.jetbrains.squash.connection.*
import org.jetbrains.squash.definition.*
import org.jetbrains.squash.drivers.*
import org.jetbrains.squash.expressions.*
import org.jetbrains.squash.graph.*
import org.jetbrains.squash.query.*
import org.jetbrains.squash.results.*
import org.jetbrains.squash.statements.*
import org.openjdk.jmh.annotations.*

object LoadTable : TableDefinition() {
    val id = integer("id").autoIncrement().primaryKey()
    val name = varchar("name", 20)
    val value = integer("value").index()
}

interface Load {
    val name: String
    val value: Int
}

@State(Scope.Benchmark)
abstract class QueryBenchmark {
    private val rows = 100000
    lateinit private var transaction: Transaction

    abstract fun createTransaction(): Transaction

    @Setup
    fun setup() {
        transaction = createTransaction()

        withTransaction {
            connection.monitor.before {
                //println(it)
            }
            databaseSchema().create(listOf(LoadTable))
            repeat(rows) { seq ->
                insertInto(LoadTable).values {
                    it[name] = "$seq-value"
                    it[value] = seq
                }.execute()
            }
        }
    }


    @TearDown
    fun teardown() {
        transaction.close()
    }

    fun <R> withTransaction(body: suspend Transaction.() -> R): R = runBlocking { body(transaction) }
    fun <R> withJDBCTransaction(body: suspend JDBCTransaction.() -> R): R = runBlocking { body(transaction as JDBCTransaction) }


    @Benchmark
    fun iterateJdbc() = withJDBCTransaction {
        val resultSet = jdbcTransaction.prepareStatement("SELECT * FROM Load").executeQuery()
        var sum = 0
        val index = resultSet.findColumn("value")
        while (resultSet.next()) {
            sum += resultSet.getInt(index)
        }
        sum
    }

    @Benchmark
    fun iterateJdbcObject() = withJDBCTransaction {
        val resultSet = jdbcTransaction.prepareStatement("SELECT * FROM Load").executeQuery()
        var sum = 0
        val index = resultSet.findColumn("value")
        while (resultSet.next()) {
            sum += resultSet.getObject(index) as Int
        }
        sum
    }

    @Benchmark
    fun iterateJdbcName() = withJDBCTransaction {
        val resultSet = jdbcTransaction.prepareStatement("SELECT * FROM Load").executeQuery()
        var sum = 0
        while (resultSet.next()) {
            sum += resultSet.getInt("value")
        }
        sum
    }

    @Benchmark
    fun iterateQuery() = withTransaction {
        val query = from(LoadTable).select(LoadTable.name, LoadTable.value)
        val response = query.execute()
        response.sumBy { it.columnValue(LoadTable.value) }
    }

    @Benchmark
    fun iterateQueryWhere() = withTransaction {
        val query = from(LoadTable).select(LoadTable.name, LoadTable.value).where { LoadTable.value gt (rows / 2) }
        val response = query.execute()
        response.sumBy { it.columnValue(LoadTable.value) }
    }

    @Benchmark
    fun iterateMapping() = withTransaction {
        val query = from(LoadTable).select(LoadTable.name, LoadTable.value).bind<Load>(LoadTable)
        val response = query.execute()
        response.sumBy { it.value }
    }
}

