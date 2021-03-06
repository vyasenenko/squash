[![JetBrains team project](http://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Download](https://api.bintray.com/packages/kotlin/squash/squash/images/download.svg) ](https://bintray.com/kotlin/squash/squash/_latestVersion)
[![TeamCity (simple build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/KotlinTools_Squash_Build.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_Squash_Build&branch_KotlinTools_Squash=%3Cdefault%3E&tab=buildTypeStatusDiv)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![](https://jitpack.io/v/vyasenenko/squash.svg)](https://jitpack.io/#vyasenenko/squash)

Squash
------

Squash is a data access and manipulation DSL library for relational databases.

* *Strongly typed*. IDE and compiler knows how to verify your queries, assist in query editing and navigate.
* *No code generation*. Data structure definitions are done in code and validated against database actual schema.
  Schema can also be generated from definitions, which is ideal for fast prototyping and tests.
* *Unopinionated*. There is no prescribed way on how to manage your transactions, connections, or data objects.
* *Extensible*. Connection, that manages SQL execution and result set mapping, can be extended to support specific database engine needs.
  Dialect, responsible for building relevant SQL statements and queries, can be overriden or replaced to support specific SQL language variants.
* *Kotlin*.


This fork optimized for postgres
-------------
Appended functions for append, rename

```kotlin
// Alter table queries by default true
Squash.alterTable = true
```

Database migrations
-------------
Change log statement example
```kotlin
object ExampleChangeLog : ChangeLogStatement(name = "your_custom_name", changing = {
    this[ "YOUR CUSTOM" ]
    this[ "QUERIES" ]
})
```

```kotlin
connection.transaction {
    databaseSchema().changeLogController.execute(ExampleChangeLogs)
    databaseSchema().create(TableDefinitions)
}
```

Added UPSERT
-------------
Insert or update (on conflict)
```kotlin
object Names : TableDefinition("Names") {
    val firstName = varchar("firstname", 10).primaryKey()
    val lastName = varchar("lastname", 10)
    val middleName = varchar("middleName", 10)
}

val updates = insertInto(Names).values {
    it[firstName] = "Foo"
    it[lastName] = "Bar2"
    it[middleName] = "Or2"
}.onConflict(Names.firstName).update {
    it[lastName]
    it[middleName]
}.execute()

```

 
Quick Samples
-------------

Define tables:

```kotlin
object Cities : TableDefinition() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
}

object Citizens : TableDefinition() {
    val id = varchar("id", 10).primaryKey()
    val name = varchar("name", length = 50)
    val cityId = reference(Cities.id, "city_id").nullable()
}
```

Insert data:

```kotlin
insertInto(Citizens).values {
    it[id] = "eugene"
    it[name] = "Eugene"
    it[cityId] = munichId
}.execute()
```

Query tables:

```kotlin
val row = from(Citizens)
    .where { Citizens.id eq "eugene" }
    .select(Citizens.name, Citizens.id)
    .execute()
    .single()

assertEquals("eugene", row[Citizens.id])
assertEquals("Eugene", row[Citizens.name])
```

Join:

```kotlin
from(Citizens)
    .innerJoin(Cities) { Cities.id eq Citizens.cityId }
    .select(Citizens.name, Cities.name)
```
