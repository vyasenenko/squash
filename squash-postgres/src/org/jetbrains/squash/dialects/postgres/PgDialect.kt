package org.jetbrains.squash.dialects.postgres

import org.jetbrains.squash.definition.*
import org.jetbrains.squash.dialect.*

object PgDialect : BaseSQLDialect("Postgres") {

    override val columnTypes
        get() = PgTypes.values().associateBy { it.name }

    override val definition: DefinitionSQLDialect = object : BaseDefinitionSQLDialect(this) {
        override fun columnTypeSQL(builder: SQLStatementBuilder, column: Column<*>) {
            if (column.hasProperty<AutoIncrementProperty>()) {
                require(!column.hasProperty<NullableProperty>()) { "Column ${column.name} cannot be both AUTOINCREMENT and NULL" }
                val type = column.type
                val autoincrement = when (type) {
                    is IntColumnType -> "SERIAL"
                    is LongColumnType -> "BIGSERIAL"
                    else -> error("AutoIncrement column for '$type' is not supported by $this")
                }
                builder.append(autoincrement)
            } else super.columnTypeSQL(builder, column)
        }

        override fun columnAutoIncrementProperty(builder: SQLStatementBuilder, property: AutoIncrementProperty?) {
            // do nothing, we already handled AutoIncrementProperty as SERIAL
        }

        override fun columnTypeSQL(builder: SQLStatementBuilder, type: ColumnType) {
            when (type) {
                is UUIDColumnType -> builder.append("UUID")
                is BlobColumnType -> builder.append("BYTEA")
                is BinaryColumnType -> builder.append("BYTEA")
                is DateTimeColumnType -> builder.append("TIMESTAMP")
                else -> super.columnTypeSQL(builder, type)
            }
        }
    }
}