package org.jetbrains.squash.drivers

import org.jetbrains.squash.results.get
import org.jetbrains.squash.schema.DatabaseSchema
import org.jetbrains.squash.schema.DatabaseSchemaBase
import java.sql.DatabaseMetaData

open class JDBCDatabaseSchema(final override val transaction: JDBCTransaction) : DatabaseSchemaBase(transaction) {
    protected val catalogue: String? = transaction.jdbcTransaction.catalog
    protected val metadata: DatabaseMetaData = transaction.jdbcTransaction.metaData

    override fun tables(): Sequence<DatabaseSchema.SchemaTable> {
        val resultSet = metadata.getTables(catalogue, currentSchema(), null, arrayOf("TABLE"))
        return JDBCResponse(transaction.connection.conversion, resultSet).rows.map { SchemaTable(it["TABLE_NAME"], this) }
    }

    protected open fun currentSchema(): String = transaction.jdbcTransaction.schema ?: ""

    class SchemaColumn(override val name: String,
                       override val nullable: Boolean,
                       override val type: String,
                       override val size: Int?) : DatabaseSchema.SchemaColumn {
        override fun toString(): String = "$name:$type${if (nullable) "?" else ""} ($size)"
    }

    class SchemaTable(override val name: String, private val schema: JDBCDatabaseSchema) : DatabaseSchema.SchemaTable {
        override fun columns(): Sequence<DatabaseSchema.SchemaColumn> {
            val resultSet = schema.metadata.getColumns(schema.catalogue, schema.currentSchema(), name, null)
            val response = JDBCResponse(schema.transaction.connection.conversion, resultSet)
            return response.rows.map {
                SchemaColumn(it["COLUMN_NAME"], it.get<Int>("NULLABLE") == DatabaseMetaData.columnNullable,
                        it["TYPE_NAME"], it.get<Int>("COLUMN_SIZE"))
            }
        }

        override fun toString(): String = "[JDBC] Table: $name"
    }
}