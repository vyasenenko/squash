package org.jetbrains.squash.dialect

import org.jetbrains.squash.definition.*
import org.jetbrains.squash.schema.DatabaseSchema

interface DefinitionSQLDialect {
    fun tableSQL(table: TableDefinition): List<SQLStatement>
    fun foreignKeys(table: TableDefinition): List<SQLStatement>
    fun alterTable(table: TableDefinition, schemas: List<DatabaseSchema.SchemaTable>): List<SQLStatement>
}