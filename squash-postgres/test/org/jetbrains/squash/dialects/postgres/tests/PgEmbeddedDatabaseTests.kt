package org.jetbrains.squash.dialects.postgres.tests

import org.jetbrains.squash.connection.DatabaseConnection
import org.jetbrains.squash.definition.ColumnType
import org.jetbrains.squash.definition.IntColumnType
import org.jetbrains.squash.definition.LongColumnType
import org.jetbrains.squash.dialects.postgres.PgConnection
import org.jetbrains.squash.tests.DatabaseTests
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import java.nio.file.Paths
import kotlin.test.fail


class PgEmbeddedDatabaseTests : DatabaseTests {
    override val quote = "\""
    override val blobType = "BYTEA"
    override fun getIdColumnType(columnType: ColumnType): String = when (columnType) {
        is IntColumnType -> "SERIAL NOT NULL"
        is LongColumnType -> "BIGSERIAL NOT NULL"
        else -> fail("Unsupported column type $columnType")
    }

    override fun primaryKey(name: String, vararg column: String): String = ", CONSTRAINT PK_$name PRIMARY KEY (${column.joinToString()})"
    override fun autoPrimaryKey(table: String, column: String): String = primaryKey(table, column)

    override fun createConnection(): DatabaseConnection {
        return PgConnection.create(urlPg)
    }

    override fun createTransaction() = createConnection().createTransaction().apply {
        executeStatement("SET search_path TO pg_temp")
    }

    companion object {
        private val urlPg = EmbeddedPostgres().start(EmbeddedPostgres.cachedRuntimeConfig(Paths.get("target/pg_embedded")))
    }
}