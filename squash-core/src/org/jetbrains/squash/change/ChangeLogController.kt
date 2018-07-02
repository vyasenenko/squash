package org.jetbrains.squash.change

import org.jetbrains.squash.connection.Transaction
import org.jetbrains.squash.expressions.eq
import org.jetbrains.squash.query.from
import org.jetbrains.squash.query.select
import org.jetbrains.squash.query.where
import org.jetbrains.squash.results.get
import org.jetbrains.squash.schema.create
import org.jetbrains.squash.schema.drop
import org.jetbrains.squash.statements.deleteFrom
import org.jetbrains.squash.statements.insertInto
import org.jetbrains.squash.statements.values

/**
 * Source control system for custom query.
 * *
 * @author Vitaliy Yasenenko.
 */
class ChangeLogController(val transaction: Transaction) {

    init {
        transaction.databaseSchema().create(ChangeLogTable)
    }

    fun execute(vararg changeLogs: ChangeLogStatement): List<ChangedData> {
        val changes = getAllChanges()

        changeLogs.forEach { changeLog ->

            changeLog.list.forEach { changeStatement ->
                val existChange = changes.find { it.vid == changeStatement.vid && it.name == changeLog.nameChangeLog }
                when {
                    existChange == null -> {
                        transaction.executeStatement(changeStatement.query)
                        insertInto(ChangeLogTable).values {
                            it[vid] = changeStatement.vid
                            it[name] = changeLog.nameChangeLog
                            it[query] = changeStatement.query
                        }.executeOn(transaction)
                    }
                    existChange.query != changeStatement.query ->
                        error("""Query was changed $existChange
                                         new query '${changeLog.nameChangeLog}': $changeStatement""")
                }
            }
        }

        return getAllChanges()
    }

    private fun getAllChanges() = from(ChangeLogTable)
            .select(ChangeLogTable.vid, ChangeLogTable.name, ChangeLogTable.query, ChangeLogTable.whenChanged)
            .executeOn(transaction)
            .map {
                ChangedData(it[ChangeLogTable.vid], it[ChangeLogTable.name], it[ChangeLogTable.query], it[ChangeLogTable.whenChanged])
            }.toList()

    fun dropChangeLogTable() {
        transaction.databaseSchema().drop(ChangeLogTable)
    }

    fun clearChangeLog(changeLog: ChangeLogStatement) {
        deleteFrom(ChangeLogTable).where { ChangeLogTable.name eq changeLog.nameChangeLog }.executeOn(transaction)
    }
}