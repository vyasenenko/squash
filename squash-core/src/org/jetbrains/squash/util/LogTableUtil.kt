package org.jetbrains.squash.util

import org.jetbrains.squash.definition.TableDefinition
import org.jetbrains.squash.schema.DatabaseSchema
import org.jetbrains.squash.schema.DatabaseSchemaBase

fun DatabaseSchemaBase.logTable(tables: List<TableDefinition>) {
    val mapTables = tables.associateBy { it.compoundName.id.toLowerCase() }
    val maxLength = tables.maxBy { it.compoundColumns.maxBy { it.toString().length }.toString().length }!!
            .compoundColumns.maxBy { it.toString().length }.toString().length + 20

    transaction.connection.dialect.definition.constrains(transaction)
    val tablesFromSchema = transaction.databaseSchema().tables()

    (if (tablesFromSchema.any()) "\u001B[1;34m[DATABASE SCHEMA]\u001B[0m\n" else "\u001B[1;31mYou don't have any tables!!\u001B[0m").log()
    tablesFromSchema.forEach { exist ->

        "Table [${exist.name.toUpperCase()}]".log()

        with(mapTables[exist.name.toLowerCase()]) {
            if (this == null) {
                exist.log { "Columns: ${this.columns().joinToString()}" }

                "Not found table definition ${exist.name}".log()
            } else {
                "\tDefinitions${(18..maxLength).joinToString("") { " " }}Columns".log()
                val columns = exist.columns().map { Check(false, it) }.associateBy { it.schemaColumn.name.toLowerCase() }
                compoundColumns.forEach { columnDefinition ->
                    val findExistColumn = columns[columnDefinition.name.id.toLowerCase()]
                    if (findExistColumn == null) {
                        "[$columnDefinition]".log()
                    } else {
                        findExistColumn.bol = true
                        (0..200).joinToString("") { " " }.let {
                            val def = columnDefinition.toString()
                            val col = findExistColumn.schemaColumn.toString()
                            it.replaceRange(6, 6 + def.length - 1, def)
                                    .replaceRange(6 + def.length, maxLength, (def.length - 1..maxLength)
                                            .joinToString("") { "_" })
                                    .replaceRange(maxLength, maxLength + col.length, col)
                        }.log()
                    }
                }
                val values = columns.filter { !it.value.bol }.values
                if (values.isNotEmpty()) "Not found definitions: ".log()
                ("\t" + values.joinToString(" | ") { it.schemaColumn.toString() }).log()
                "".log()
            }
        }
    }
}

private data class Check(var bol: Boolean, val schemaColumn: DatabaseSchema.SchemaColumn)
