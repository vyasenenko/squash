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

