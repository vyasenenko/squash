package org.jetbrains.squash.tests

import org.jetbrains.squash.schema.create
import org.jetbrains.squash.schema.drop
import org.jetbrains.squash.tests.data.*
import kotlin.test.*

/**
 * Alter table queries tests.
 *
 * @author Vitaliy Yasenenko
 */
abstract class DataBaseModificationTest : DatabaseTests {


    @BeforeTest
    fun before() {
        createTransaction().databaseSchema().create(TestTable)
    }

    @AfterTest
    fun after() {
        createTransaction().databaseSchema().drop(TestTable)
    }


    @Test
    fun checkDropTableQuery() = withTransaction {
        connection.dialect.definition.drop(TestTable).assertSQL {
            "DROP TABLE test_table"
        }
    }


    @Test
    fun checkAppendNewColumnQuery() = withTransaction {
        val schemas = databaseSchema().tables().toList()
        connection.dialect.definition.alterTable(TestTableWithNewField, schemas).assertSQL {
            "ALTER TABLE test_table ADD COLUMN \"decimal\" DECIMAL(10, 10) NOT NULL"
        }
    }


    @Test
    fun checkRenameColumnQuery() = withTransaction {
        val schemas = databaseSchema().tables().toList()
        connection.dialect.definition.alterTable(TestTableRenameField, schemas).assertSQL {
            "ALTER TABLE test_table RENAME COLUMN number TO number_new"
        }
    }


    @Test
    fun checkAppendTimestampNowQuery() = withTransaction {
        val schemas = databaseSchema().tables().toList()
        connection.dialect.definition.alterTable(TestTableTimestampNow, schemas).assertSQL {
            "ALTER TABLE test_table ADD COLUMN \"timestamp\" TIMESTAMP NOT NULL DEFAULT current_timestamp"
        }
    }


    @Test
    fun checkChangeTypeQuery() = withTransaction {
        val schemas = databaseSchema().tables().toList()
        connection.dialect.definition.alterTable(TestTableChangeType, schemas).assertSQL {
            """
                ALTER TABLE test_table ALTER COLUMN varchar TYPE INT USING varchar::INT
                ALTER TABLE test_table ALTER COLUMN number TYPE VARCHAR(20)
            """
        }
    }

    @Test
    fun checkChangeSizeQuery() = withTransaction {
        val schemas = databaseSchema().tables().toList()
        connection.dialect.definition.alterTable(TestTableChangeSize, schemas).assertSQL {
            "ALTER TABLE test_table ALTER COLUMN varchar TYPE VARCHAR(200)"
        }
    }

}