package org.jetbrains.squash.dialect

import org.jetbrains.squash.connection.Transaction
import org.jetbrains.squash.definition.*
import org.jetbrains.squash.schema.DatabaseSchema

interface DefinitionSQLDialect {

    fun tableSQL(table: TableDefinition): List<SQLStatement>
    fun foreignKeys(table: TableDefinition, constrains: List<String>): List<SQLStatement>
    fun alterTable(table: TableDefinition, schemas: List<DatabaseSchema.SchemaTable>): List<SQLStatement>
    fun constrains(transaction: Transaction): List<String>
    fun drop(table: TableDefinition): SQLStatement
}