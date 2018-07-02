package org.jetbrains.squash.schema

import org.jetbrains.squash.change.ChangeLogController
import org.jetbrains.squash.definition.*
import org.jetbrains.squash.dialect.*

/**
 * Provides facilities for querying and modifying database schema
 */
interface DatabaseSchema {

    fun tables(): Sequence<SchemaTable>

    fun create(tables: List<TableDefinition>)
    fun createStatements(tables: List<TableDefinition>): List<SQLStatement>
    fun drop(tables: List<TableDefinition>)

    val changeLogController: ChangeLogController

    data class DatabaseSchemaValidationItem(val message: String)

    fun validate(tables: List<org.jetbrains.squash.definition.Table>): List<DatabaseSchemaValidationItem>

    interface SchemaTable {
        val name: String
        fun columns(): Sequence<SchemaColumn>
    }

    interface SchemaColumn {
        val name: String
        val nullable: Boolean
        val type: String
        val size: Int?
    }
}

fun DatabaseSchema.create(vararg tables: TableDefinition) = create(tables.asList())
fun DatabaseSchema.drop(vararg tables: TableDefinition) = drop(tables.asList())