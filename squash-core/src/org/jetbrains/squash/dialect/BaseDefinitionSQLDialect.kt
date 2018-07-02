package org.jetbrains.squash.dialect

import org.jetbrains.squash.connection.Transaction
import org.jetbrains.squash.definition.*
import org.jetbrains.squash.util.Loggable
import org.jetbrains.squash.results.get
import org.jetbrains.squash.schema.DatabaseSchema

open class BaseDefinitionSQLDialect(val dialect: SQLDialect) : DefinitionSQLDialect, Loggable {

    override fun tableSQL(table: TableDefinition): List<SQLStatement> {
        val tableSQL = SQLStatementBuilder().apply {
            append("CREATE TABLE IF NOT EXISTS ${dialect.idSQL(table.compoundName)}")
            if (table.compoundColumns.any()) {
                append(" (")
                table.compoundColumns.forEachIndexed { index, column ->
                    if (index > 0)
                        append(", ")
                    columnDefinitionSQL(this, column)
                }

                appendPrimaryKey(this, table)
                append(")")
            }
        }.build()
        val indices = indicesSQL(table)
        return listOf(tableSQL) + indices
    }

    override fun foreignKeys(table: TableDefinition, constrains: List<String>): List<SQLStatement> =
            table.constraints.elements.filterIsInstance<ForeignKeyConstraint>()
                    .mapNotNull { key ->
                        if (constrains.contains(key.name.id.toLowerCase())) {
                            null
                        } else {
                            SQLStatementBuilder().apply {
                                alterTable(table)
                                append("ADD ")
                                appendForeignKey(this, key)
                            }.build()
                        }
                    }

    override fun alterTable(table: TableDefinition, schemas: List<DatabaseSchema.SchemaTable>): List<SQLStatement> {
        val existTable = schemas.associateBy { it.name.toLowerCase() }
        val nameSQL = dialect.nameSQL(table.compoundName).toLowerCase()
        val exist = existTable[nameSQL]
        return if (exist != null) {
            table.compoundColumns.mapNotNull { column ->
                val find = exist.columns().find { column.name.identifier.id.toLowerCase() == it.name.toLowerCase() }

                if (find == null) {
                    SQLStatementBuilder().apply {
                        val renameColumnProperty = column.propertyOrNull<RenameColumnProperty>()
                        when (renameColumnProperty) {
                            null -> {
                                alterTable(table)
                                append("ADD COLUMN ")
                                append(dialect.idSQL(column.name))
                                append(" ")
                                columnTypeSQL(this, column)
                                columnPropertiesSQL(this, column)
                                "Added column $column".log()
                            }
                            else -> {
                                alterTable(table)
                                append("RENAME COLUMN ")
                                append("${dialect.idSQL(Identifier(renameColumnProperty.oldName))} TO ${dialect.idSQL(column.name)}")
                                "Renamed column $column from ${renameColumnProperty.oldName}".log()
                            }
                        }
                    }.build()
                } else {
                    val columnTypeDB = dialect.columnTypes[find.type]
                            ?: error("This ${find.type} column type db not support in DSL")
                    val columnType = column.type
                    when {
                        !columnTypeDB.classes.contains(columnType::class) -> {
                            SQLStatementBuilder().apply {
                                alterTable(table)
                                append("ALTER COLUMN ${find.name} TYPE ")
                                columnTypeSQL(this, column)
                                if (listOf(IntColumnType::class,
                                                LongColumnType::class,
                                                EnumColumnType::class).contains(columnType::class)) {
                                    append(" USING ${find.name}::")
                                    columnTypeSQL(this, column)
                                }
                                "Changed type $find to $column".log()
                            }.build()
                        }
                        find.type == "varchar" && columnType is StringColumnType && columnType.length != find.size ->
                            SQLStatementBuilder().apply {
                                alterTable(table)
                                append("ALTER COLUMN ${find.name} TYPE ")
                                columnTypeSQL(this, column)
                                "Changed size $find to $column".log()
                            }.build()
                        else -> null
                    }
                }
            }

        } else emptyList()
    }

    override fun constrains(transaction: Transaction): List<String> = transaction.executeStatement(SQLStatement("""
			|SELECT tc.constraint_name, tc.table_name, kcu.column_name, ccu.table_name AS foreign_table_name, ccu.column_name AS foreign_column_name
			|FROM information_schema.table_constraints AS tc
    		|JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name
    		|JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name
			|WHERE constraint_type = 'FOREIGN KEY'
		""".trimMargin().replace("\n", " "), emptyList()))
            .toList().map { (it["constraint_name"] ?: "").toLowerCase() }.filter { it.isNotBlank() }

    protected open fun indicesSQL(table: TableDefinition): List<SQLStatement> =
            table.constraints.elements.filterIsInstance<IndexConstraint>().map {
                SQLStatementBuilder().apply {
                    val unique = if (it.unique) " UNIQUE" else ""
                    append("CREATE$unique INDEX IF NOT EXISTS ${dialect.idSQL(it.name)} ON ${dialect.idSQL(table.compoundName)} (")
                    it.columns.forEachIndexed { index, column ->
                        if (index > 0)
                            append(", ")
                        append(dialect.idSQL(column.name))
                    }
                    append(")")
                }.build()
            }

    protected open fun appendPrimaryKey(builder: SQLStatementBuilder, table: TableDefinition) {
        val primaryKey = table.constraints.primaryKey ?: createAutoPrimaryKeyConstraint(table)
        if (primaryKey != null) {
            primaryKeyDefinitionSQL(builder, primaryKey, table)
        }
    }

    protected open fun createAutoPrimaryKeyConstraint(table: TableDefinition): PrimaryKeyConstraint? {
        val autoIncrement = table.compoundColumns.filter { it.hasProperty<AutoIncrementProperty>() }
        if (autoIncrement.any()) {
            val name = Identifier("PK_${dialect.nameSQL(table.compoundName)}")
            val primaryKey = PrimaryKeyConstraint(name, autoIncrement)
            table.constraints.primaryKey = primaryKey
            return primaryKey
        }
        return null
    }

    protected open fun primaryKeyDefinitionSQL(builder: SQLStatementBuilder, key: PrimaryKeyConstraint, table: Table) = with(builder) {
        append(", ")
        append("CONSTRAINT ${dialect.idSQL(key.name)} PRIMARY KEY (")
        append(key.columns.joinToString { dialect.idSQL(it.name) })
        append(")")
    }

    protected open fun appendForeignKey(builder: SQLStatementBuilder, key: ForeignKeyConstraint) = with(builder) {
        append("CONSTRAINT ${dialect.idSQL(key.name)} FOREIGN KEY (")
        append(key.sources.joinToString { dialect.idSQL(it.name) })
        val destinationTable = key.destinations.first().compound
        append(") REFERENCES ${dialect.nameSQL(destinationTable.compoundName)}(")
        append(key.destinations.joinToString { dialect.idSQL(it.name) })
        append(")")
    }

    protected open fun columnDefinitionSQL(builder: SQLStatementBuilder, column: Column<*>): Unit = with(builder) {
        append(dialect.idSQL(column.name))
        append(" ")
        columnTypeSQL(this, column)
        columnPropertiesSQL(this, column)
    }

    protected open fun columnTypeSQL(builder: SQLStatementBuilder, column: Column<*>): Unit = with(builder) {
        when (column) {
            is ReferenceColumn -> {
                columnTypeSQL(this, column.reference.type)
            }

            is ColumnDefinition -> {
                columnTypeSQL(this, column.type)
            }

            is DialectExtension -> {
                column.appendTo(this, dialect)
            }
            else -> error("Column class '${column.javaClass.simpleName}' is not supported by $this")
        }
    }

    protected open fun columnPropertiesSQL(builder: SQLStatementBuilder, column: Column<*>): Unit = with(builder) {
        require(!column.hasProperty<NullableProperty>() || !column.hasProperty<AutoIncrementProperty>()) {
            "Column ${column.name} cannot be both AUTOINCREMENT and NULL"
        }
        columnNullableProperty(builder, column.propertyOrNull())
        columnAutoIncrementProperty(builder, column.propertyOrNull())
        columnDefaultProperty(builder, column.propertyOrNull())
        columnNowProperty(builder, column.propertyOrNull())
    }

    private fun columnNowProperty(builder: SQLStatementBuilder, property: NowTimeProperty?) {
        if (property != null) {
            builder.append(" DEFAULT current_timestamp")
        }
    }

    open fun columnNullableProperty(builder: SQLStatementBuilder, property: NullableProperty?) {
        if (property != null)
            builder.append(" NULL")
        else
            builder.append(" NOT NULL")
    }

    open fun columnAutoIncrementProperty(builder: SQLStatementBuilder, property: AutoIncrementProperty?) {
        if (property != null)
            builder.append(" AUTO_INCREMENT")
    }

    open fun columnDefaultProperty(builder: SQLStatementBuilder, property: DefaultValueProperty<*>?) {
        if (property != null) {
            builder.append(" DEFAULT ")
            dialect.appendLiteralSQL(builder, property.value)
        }
    }

    protected open fun columnTypeSQL(builder: SQLStatementBuilder, type: ColumnType): Unit = with(builder) {
        when (type) {
            is CharColumnType -> append("CHAR")
            is LongColumnType -> append("BIGINT")
            is IntColumnType -> append("INT")
            is DecimalColumnType -> append("DECIMAL(${type.scale}, ${type.precision})")
            is EnumColumnType -> append("INT")
            is DateColumnType -> append("DATE")
            is DateTimeColumnType -> append("DATETIME")
            is BinaryColumnType -> append("VARBINARY(${type.length})")
            is UUIDColumnType -> append("BINARY(16)")
            is BooleanColumnType -> append("BOOLEAN")
            is BlobColumnType -> append("BLOB")
            is StringColumnType -> {
                val sqlType = when (type.length) {
                    in 1..255 -> "VARCHAR(${type.length})"
                    else -> "TEXT"
                }
                if (type.collate == null)
                    append(sqlType)
                else
                    append(sqlType + " COLLATE ${type.collate}")
            }
            is DialectExtension -> type.appendTo(builder, dialect)
            else -> error("Column type '$type' is not supported by $this")
        }
    }

    private fun SQLStatementBuilder.alterTable(table: TableDefinition) {
        append("ALTER TABLE ${dialect.nameSQL(table.compoundName)} ")
    }

    override fun drop(table: TableDefinition): SQLStatement {
        return SQLStatementBuilder().apply {
            append("DROP TABLE ")
            append(dialect.nameSQL(table.compoundName))
        }.build()
    }
}