package org.jetbrains.squash.tests.data

import org.jetbrains.squash.change.ChangeLogController
import org.jetbrains.squash.change.ChangeLogStatement
import org.jetbrains.squash.connection.Transaction
import org.jetbrains.squash.tests.DatabaseTests

private const val name = "test_change_log"

object TestChangeLog : ChangeLogStatement(name, {
    this["CREATE TABLE test_change_log_table (id SERIAL NOT NULL CONSTRAINT pk_test_change_log_table PRIMARY KEY, name VARCHAR(20) NOT NULL)"]
    this["ALTER TABLE test_change_log_table ADD COLUMN date TIMESTAMP"]
    this["DROP TABLE test_change_log_table"]
})

object TestChangeLogAppendQuery : ChangeLogStatement(name, {
    this["CREATE TABLE test_change_log_table (id SERIAL NOT NULL CONSTRAINT pk_test_change_log_table PRIMARY KEY, name VARCHAR(20) NOT NULL)"]
    this["ALTER TABLE test_change_log_table ADD COLUMN date TIMESTAMP"]
    this["DROP TABLE test_change_log_table"]
    this["SELECT t.* FROM database_change_log t LIMIT 502"]
})

object TestChangeLogIllegal : ChangeLogStatement(name, {
    this["NOT EQUALS QUERY"]
    this["ALTER TABLE test_change_log_table ADD COLUMN date TIMESTAMP"]
    this["DROP TABLE test_change_log_table"]
})

object TestChangeLogNotValid : ChangeLogStatement(changing =  {
    this["NOT VALID QUERY"]
})

fun <R> DatabaseTests.withChangeLog(statement: Transaction.(sut: ChangeLogController) -> R) :R = this.withTransaction {
    statement(databaseSchema().changeLogController)
}