package org.jetbrains.squash.tests

import org.jetbrains.squash.schema.create
import org.jetbrains.squash.tests.data.*
import kotlin.test.Test

/**
 * Alter table queries tests.
 *
 * @author Vitaliy Yasenenko
 */
abstract class DatabaseModificationTest : DatabaseTests {

    @Test
    fun checkDropTableQuery() = withTransaction {
        databaseSchema().create(TestTable)
        connection.dialect.definition.drop(TestTable).assertSQL {
            "DROP TABLE test_table"
        }
    }


    @Test
    fun checkAppendNewColumnQuery() = withTransaction {
        databaseSchema().create(TestTable)
        val schemas = databaseSchema().tables().toList()
        connection.dialect.definition.alterTable(TestTableWithNewField, schemas).assertSQL {
            "ALTER TABLE test_table ADD COLUMN \"decimal\" DECIMAL(10, 10) NOT NULL"
        }
    }

    @Test
    fun checkCreateReferenceColumn() = withTransaction {

        databaseSchema().create(TestTable, WithoutReferenceTable)

        val constrains = connection.dialect.definition.constrains(this)

        connection.dialect.definition.alterTable(WithReferenceTable, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE ref_test_table ADD COLUMN test_table_id BIGINT NOT NULL"
        }
        connection.dialect.definition.foreignKeys(WithReferenceTable, constrains).assertSQL {
            "ALTER TABLE ref_test_table ADD CONSTRAINT FK_ref_test_table_test_table_id FOREIGN KEY (test_table_id) REFERENCES test_table(id)"
        }

        val checkQuery = CheckQuery(this)

        databaseSchema().create(TestTable, WithReferenceTable)

        checkQuery.assert(3, "[1] all constraint", "[2] expected query alter table add column", "[3] alter table add constraint")
    }

    @Test
    fun checkRenameColumnQuery() = withTransaction {
        databaseSchema().create(TestTable)

        connection.dialect.definition.alterTable(TestTableRenameField, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_table RENAME COLUMN number TO number_new"
        }
    }


    @Test
    fun checkAppendTimestampNowQuery() = withTransaction {
        databaseSchema().create(TestTable)
        val schemas = databaseSchema().tables().toList()
        connection.dialect.definition.alterTable(TestTableTimestampNow, schemas).assertSQL {
            "ALTER TABLE test_table ADD COLUMN \"timestamp\" TIMESTAMP NOT NULL DEFAULT current_timestamp"
        }
    }


    @Test
    fun checkChangeTypeQuery() = withTransaction {
        databaseSchema().create(TestTable)
        val schemas = databaseSchema().tables().toList()
        connection.dialect.definition.alterTable(TestTableChangeType, schemas).assertSQL {
            """
                ALTER TABLE test_table ALTER COLUMN varchar TYPE INT USING varchar::INT
                ALTER TABLE test_table ALTER COLUMN number TYPE VARCHAR(20)
            """
        }
    }



}