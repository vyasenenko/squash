package org.jetbrains.squash.tests.data

import org.jetbrains.squash.definition.*

private const val nameTable = "test_table"

object TestTable : TableDefinition(nameTable) {

    val id = long("id").autoIncrement().primaryKey()
    val time = datetime("time")
    val varchar = varchar("varchar", 100)
    val number = integer("number")
}

object TestTableChangeSize : TableDefinition(nameTable) {

    val id = long("id").autoIncrement().primaryKey()
    val time = datetime("time")
    val varchar = varchar("varchar", 200)
    val number = integer("number")
}

object TestTableChangeType : TableDefinition(nameTable) {

    val id = long("id").autoIncrement().primaryKey()
    val time = datetime("time")
    val varchar = integer("varchar")
    val number = varchar("number", 20)
}

object TestTableWithNewField : TableDefinition(nameTable) {

    val id = long("id").autoIncrement().primaryKey()
    val time = datetime("time")
    val varchar = varchar("varchar", 100)
    val number = integer("number")
    val decimal = decimal("decimal", 10, 10)
}

object TestTableRenameField : TableDefinition(nameTable) {

    val id = long("id").autoIncrement().primaryKey()
    val time = datetime("time")
    val varchar = varchar("varchar", 100)
    val number = integer("number_new").rename("number")
}

object TestTableTimestampNow : TableDefinition(nameTable) {

    val id = long("id").autoIncrement().primaryKey()
    val time = datetime("time")
    val varchar = varchar("varchar", 100)
    val number = integer("number")
    val timestamp = datetime("timestamp").now()
}

// Test data for transform type column from long to enum
private const val longTo = "test_change_type"

object LongTable: TableDefinition(longTo) {
    val long = long("name")
}

object ReferenceTable: TableDefinition(longTo) {
    val long = reference(TestTable.id, "name")
}

object ReferenceWitoutNameTable: TableDefinition(longTo) {
    val long = reference(TestTable.id)
}

object IntTable: TableDefinition(longTo) {
    val int = integer("name")
}

object TextTable: TableDefinition(longTo) {
    val string = text("name")
}

object EnumTable: TableDefinition(longTo) {
    val enumeration = enumeration<TestEnum>("name")
}

enum class TestEnum {
    COLD, HOT
}

// Test data for create reference column
private const val refName = "ref_test_table"

object WithoutReferenceTable : TableDefinition(refName) {

    val id = long("id").autoIncrement().primaryKey()
}

object WithReferenceTable : TableDefinition(refName) {

    val id = long("id").autoIncrement().primaryKey()
    val testId = reference(TestTable.id)
}