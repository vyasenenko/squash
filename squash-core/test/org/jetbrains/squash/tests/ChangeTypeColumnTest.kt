package org.jetbrains.squash.tests

import org.jetbrains.squash.connection.Transaction
import org.jetbrains.squash.definition.TableDefinition
import org.jetbrains.squash.schema.create
import org.jetbrains.squash.statements.insertInto
import org.jetbrains.squash.statements.values
import org.jetbrains.squash.tests.data.*
import org.junit.ComparisonFailure
import kotlin.test.Test

abstract class ChangeTypeColumnTest : DatabaseTests {

    @Test
    fun checkLongToEnum() = withTables(LongTable) {

        val to = EnumTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE INT USING name::INT"
        }

        executeChangeType(to)
    }

    @Test
    fun checkLongToInt() = withTables(LongTable) {

        val to = IntTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE INT USING name::INT"
        }

        executeChangeType(to)
    }

    @Test
    fun checkLongToText() = withTables(LongTable) {

        val to = TextTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE TEXT"
        }

        executeChangeType(to)
    }

    @Test
    fun checkIntToReference() = withTables(TestTable, IntTable) {

        val to = ReferenceTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE BIGINT USING name::BIGINT"
        }
        connection.dialect.definition.foreignKeys(to, connection.dialect.definition.constrains(this)).assertSQL {
            "ALTER TABLE test_change_type ADD CONSTRAINT FK_test_change_type_name FOREIGN KEY (name) REFERENCES test_table(id)"
        }

        val checkQuery = CheckQuery(this)

        databaseSchema().create(to)

        checkQuery.assert(3, "[1] all constraint", "[2] expected query alter table add column", "[3] alter table add constraint")
    }

    @Test(ComparisonFailure::class)
    fun checkIntToReferenceWithoutName() = withTables(TestTable, IntTable) {

        val to = ReferenceWitoutNameTable

        // Will be add column!!
        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE BIGINT USING name::BIGINT"
        }
        connection.dialect.definition.foreignKeys(to, connection.dialect.definition.constrains(this)).assertSQL {
            "ALTER TABLE test_change_type ADD CONSTRAINT FK_test_change_type_name FOREIGN KEY (name) REFERENCES test_table(id)"
        }
    }

    @Test
    fun checkIntToLong() = withTables(IntTable) {

        val to = LongTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE BIGINT USING name::BIGINT"
        }

        executeChangeType(to)
    }

    @Test
    fun checkIntToText() = withTables(IntTable) {

        val to = TextTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE TEXT"
        }

        executeChangeType(to)
    }

    @Test
    fun checkIntToEnum() = withTables(IntTable) {

        val to = EnumTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL("")
    }

    @Test
    fun checkEnumToLong() = withTables(EnumTable) {

        val to = LongTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE BIGINT USING name::BIGINT"
        }

        executeChangeType(to)
    }


    @Test
    fun checkTextToLong() = withTables(TextTable) {

        val to = LongTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE BIGINT USING name::BIGINT"
        }

        executeChangeType(to)
    }

    @Test(expected = Exception::class)
    fun checkChangeTypeWithInvalidDataFromTextToLong() = withTables(TextTable) {

        insertInto(TextTable).values {
            it[string] = "test"
        }.execute()

        val to = LongTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE BIGINT USING name::BIGINT"
        }

        executeChangeType(to)
    }

    @Test
    fun changeTypeWithValidDataFromTextToLong()= withTables(TextTable) {

        insertInto(TextTable).values {
            it[string] = "222"
        }.execute()

        val to = LongTable

        connection.dialect.definition.alterTable(to, databaseSchema().tables().toList()).assertSQL {
            "ALTER TABLE test_change_type ALTER COLUMN name TYPE BIGINT USING name::BIGINT"
        }

        executeChangeType(to)
    }

    @Test
    fun checkChangeSizeQuery() = withTransaction {
        databaseSchema().create(TestTable)
        val schemas = databaseSchema().tables().toList()
        connection.dialect.definition.alterTable(TestTableChangeSize, schemas).assertSQL {
            "ALTER TABLE test_table ALTER COLUMN varchar TYPE VARCHAR(200)"
        }
    }

    private fun Transaction.executeChangeType(tableDefinition: TableDefinition) {
        val checkQuery = CheckQuery(this)

        databaseSchema().create(tableDefinition)

        checkQuery.assert(2, "[1] check all constraints", "[2] Change type")
    }
}