package org.jetbrains.squash.tests

import org.jetbrains.squash.definition.TableDefinition
import org.jetbrains.squash.definition.integer
import org.jetbrains.squash.definition.primaryKey
import org.jetbrains.squash.definition.reference
import org.jetbrains.squash.definition.varchar
import org.jetbrains.squash.expressions.alias
import org.jetbrains.squash.expressions.contains
import org.jetbrains.squash.expressions.count
import org.jetbrains.squash.expressions.div
import org.jetbrains.squash.expressions.eq
import org.jetbrains.squash.expressions.gt
import org.jetbrains.squash.expressions.invoke
import org.jetbrains.squash.expressions.literal
import org.jetbrains.squash.expressions.max
import org.jetbrains.squash.expressions.minus
import org.jetbrains.squash.expressions.plus
import org.jetbrains.squash.expressions.subquery
import org.jetbrains.squash.expressions.times
import org.jetbrains.squash.query.from
import org.jetbrains.squash.query.groupBy
import org.jetbrains.squash.query.having
import org.jetbrains.squash.query.innerJoin
import org.jetbrains.squash.query.limit
import org.jetbrains.squash.query.orderBy
import org.jetbrains.squash.query.orderByDescending
import org.jetbrains.squash.query.query
import org.jetbrains.squash.query.select
import org.jetbrains.squash.query.where
import org.jetbrains.squash.results.ResultRow
import org.jetbrains.squash.results.get
import org.jetbrains.squash.statements.insertInto
import org.jetbrains.squash.statements.onConflict
import org.jetbrains.squash.statements.values
import org.jetbrains.squash.tests.data.Cities
import org.jetbrains.squash.tests.data.CitizenData
import org.jetbrains.squash.tests.data.CitizenDataLink
import org.jetbrains.squash.tests.data.Citizens
import org.jetbrains.squash.tests.data.Inhabitants
import org.jetbrains.squash.tests.data.withCities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

abstract class QueryTests : DatabaseTests {
	open fun nullsLast(sql: String): String = "$sql NULLS LAST"

	@Test
	fun selectLiteral() {
		withTables() {
			val eugene = literal("eugene")
			val query = select { eugene }

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT ?"
			}

			val row = query.execute().single()
			assertEquals(eugene.literal, row[0])
		}
	}

	@Test
	fun selectFromWhere() {
		withCities {
			val eugene = literal("eugene")
			val query = select(Citizens.name, Citizens.id).from(Citizens).where { Citizens.id eq eugene }

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name, Citizens.id FROM Citizens WHERE Citizens.id = ?"
			}

			val row = query.execute().single()
			assertEquals("eugene", row[Citizens.id])
			assertEquals("Eugene", row[Citizens.name])
		}
	}

	@Test
	fun selectFromLimit() {
		withCities {
			val query = select(Citizens.name, Citizens.id)
					.from(Citizens)
					.orderBy(Citizens.id)
					.limit(1)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name, Citizens.id FROM Citizens ORDER BY ${nullsLast("Citizens.id")} LIMIT ?"
			}

			val row = query.execute().single()
			assertEquals("alex", row[Citizens.id])
			assertEquals("Alex", row[Citizens.name])
		}
	}

	@Test
	fun selectFromLimitOffset() {
		withCities {
			val query = select(Citizens.name, Citizens.id)
					.from(Citizens)
					.orderBy(Citizens.id)
					.limit(1, 1)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name, Citizens.id FROM Citizens ORDER BY ${nullsLast("Citizens.id")} LIMIT ? OFFSET ?"
			}

			val row = query.execute().single()
			assertEquals("andrey", row[Citizens.id])
			assertEquals("Andrey", row[Citizens.name])
		}
	}

	@Test
	fun selectFromWhereIn() {
		withCities {
			val query = select(Citizens.name, Citizens.id)
					.from(Citizens)
					.where { listOf("eugene", "sergey") contains Citizens.id }
					.orderBy(Citizens.id)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name, Citizens.id FROM Citizens WHERE Citizens.id IN (?, ?) ORDER BY ${nullsLast(
						"Citizens.id")}"
			}

			val rows = query.execute().toList()
			assertEquals(2, rows.size)
			assertEquals("eugene", rows[0][Citizens.id])
			assertEquals("Eugene", rows[0][Citizens.name])
			assertEquals("sergey", rows[1][Citizens.id])
			assertEquals("Sergey", rows[1][Citizens.name])
		}
	}

	@Test
	fun selectFromAliasWhere() {
		withCities {
			val eugene = literal("eugene")
			val c = Citizens.alias("C")
			val query = from(c)
					.where { Citizens.id(c) eq eugene }
					.select(Citizens.name(c), Citizens.id(c))

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT C.name, C.id FROM Citizens AS C WHERE C.id = ?"
			}

			val row = query.execute().single()
			assertEquals("eugene", row[Citizens.id(c)])
			assertEquals("Eugene", row[Citizens.name(c)])
		}
	}

	@Test
	fun selectOperationFromWhere() {
		withCities {
			val eugene = literal("eugene")
			val query = from(Citizens)
					.where { Citizens.id eq eugene }
					.select { (Citizens.cityId + 1).alias("first") }
					.select { Citizens.cityId - 1 }
					.select { Citizens.cityId / 1 }
					.select { Citizens.cityId * 1 }

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.city_id + ? AS ${quote}first${quote}, Citizens.city_id - ?, Citizens.city_id / ?, Citizens.city_id * ? FROM Citizens WHERE Citizens.id = ?"
			}

			val row = query.execute().single()
			assertEquals(3, row.get<Int>("first"))
		}
	}

	@Test
	fun selectFromWhereSubQuery() {
		withTables {
			val eugene = literal("eugene")
			val query = select(Citizens.name).from(Citizens)
					.where {
						Citizens.id eq subquery<String> {
							from(Citizens).select { Citizens.id }.where { Citizens.id eq eugene }
						}
					}

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name FROM Citizens WHERE Citizens.id = (SELECT Citizens.id FROM Citizens WHERE Citizens.id = ?)"
			}
		}
	}

	@Test
	fun selectFromWhereWhere() {
		withTables {
			val eugene = literal("eugene")
			val query = select(Citizens.name).from(Citizens)
					.where { Citizens.id eq eugene }
					.where { Citizens.cityId eq 1 }

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name FROM Citizens WHERE Citizens.id = ? AND Citizens.city_id = ?"
			}
		}
	}

	@Test
	fun selectFromJoin() {
		withTables {
			val query = from(Citizens)
					.innerJoin(Cities) { Cities.id eq Citizens.cityId }
					.select { Citizens.name }
					.select { Cities.name }

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name, Cities.name FROM Citizens INNER JOIN Cities ON Cities.id = Citizens.city_id"
			}
		}
	}

	@Test
	fun selectFromJoinLimited() {
		withTables {
			val query = from(Citizens)
					.innerJoin(Cities) { Cities.id eq Citizens.cityId }
					.select(Citizens)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.* FROM Citizens INNER JOIN Cities ON Cities.id = Citizens.city_id"
			}
		}
	}

	@Test
	fun selectFromJoinAliased() {
		withCities {
			val citizenName = Citizens.name.alias("citizenName")
			val cityName = Cities.name.alias("city")
			val query = select(citizenName, cityName)
					.from(Citizens)
					.innerJoin(Cities) { Cities.id eq Citizens.cityId }

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name AS citizenName, Cities.name AS city FROM Citizens INNER JOIN Cities ON Cities.id = Citizens.city_id"
			}

			val rows = query.execute().toList().sortedBy { it[citizenName] }
			assertEquals(3, rows.size)
			assertEquals("Andrey", rows[0][citizenName])
			assertEquals("St. Petersburg", rows[0][cityName])
			assertEquals("Eugene", rows[1][citizenName])
			assertEquals("Munich", rows[1][cityName])
			assertEquals("Sergey", rows[2][citizenName])
			assertEquals("Munich", rows[2][cityName])
		}
	}

	@Test
	fun selectFromJoinJoin() {
		withCities {
			val query = from(Citizens)
					.innerJoin(Cities) { Cities.id eq Citizens.cityId }
					.innerJoin(CitizenDataLink) { Citizens.id eq CitizenDataLink.citizen_id }
					.innerJoin(CitizenData) { CitizenData.id eq CitizenDataLink.citizendata_id }
					.select { Citizens.name }
					.select { Cities.name }
					.select { CitizenData.image }

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name, Cities.name, CitizenData.image FROM Citizens INNER JOIN Cities ON Cities.id = Citizens.city_id INNER JOIN CitizenDataLink ON Citizens.id = CitizenDataLink.Citizens_id INNER JOIN CitizenData ON CitizenData.id = CitizenDataLink.CitizenData_id"
			}

			val rows = query.execute().toList()
			assertEquals(2, rows.count())
			assertEquals("Eugene", rows[0][Citizens.name])
			assertEquals("Munich", rows[0][Cities.name])
			assertNull(rows[0][CitizenData.image])

			assertEquals("Eugene", rows[1][Citizens.name])
			assertEquals("Munich", rows[1][Cities.name])
			assertEquals(listOf<Byte>(1, 2, 3), rows[1][CitizenData.image]?.bytes?.toList())
		}
	}

	@Test
	fun selectFromJoinJoinAdHoc() {
		val Numbers = object : TableDefinition("Numbers") {
			val id = integer("id").primaryKey()
		}

		val Names = object : TableDefinition("Names") {
			val name = varchar("name", 10).primaryKey()
		}

		val Map = object : TableDefinition("Map") {
			val id_ref = reference(Numbers.id, "id_ref")
			val name_ref = reference(Names.name, "name_ref")
		}

		withTables(Numbers, Names, Map) {
			insertInto(Numbers).values { it[id] = 1 }.execute()
			insertInto(Numbers).values { it[id] = 2 }.execute()

			insertInto(Names).values { it[name] = "Foo" }.execute()
			insertInto(Names).values { it[name] = "Bar" }.execute()

			insertInto(Map).values {
				it[id_ref] = 2
				it[name_ref] = "Foo"
			}.execute()

			val query = from(Map)
					.innerJoin(Names) { Map.name_ref eq Names.name }
					.innerJoin(Numbers) { Map.id_ref eq Numbers.id }

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT * FROM ${quote}Map${quote} INNER JOIN ${quote}Names${quote} ON ${quote}Map${quote}.name_ref = ${quote}Names${quote}.name INNER JOIN Numbers ON ${quote}Map${quote}.id_ref = Numbers.id"
			}

			val rows: List<ResultRow> = query.execute().toList()

			assertEquals(1, rows.size)
			assertEquals(2, rows[0][Numbers.id])
			assertEquals("Foo", rows[0][Names.name])
		}
	}

	@Test
	fun insertOnConflict() {


		val Names = object : TableDefinition("Names") {
			val firstName = varchar("firstname", 10).primaryKey()
			val lastName = varchar("lastname", 10)
		}

		withTables(Names) {

			insertInto(Names).values {
				it[firstName] = "Foo"
				it[lastName] = "Bar"
			}.execute()

			val updates = insertInto(Names).values {
				it[firstName] = "Foo"
				it[lastName] = "Bar2"
			}.onConflict(Names.firstName).update(Names.lastName)

			connection.dialect.statementSQL(updates).assertSQL {
				"INSERT INTO \"Names\" (firstname, lastname) VALUES (?, ?) " +
						"ON CONFLICT (firstname) DO UPDATE SET lastname = excluded.lastname;"
			}

			updates.execute()

			val rows = from(Names).execute().toList()


			assertEquals(1, rows.size)
			assertEquals("Foo", rows[0][Names.firstName])
			assertEquals("Bar2", rows[0][Names.lastName])
		}
	}

	@Test
	fun insertOnConflictOneFieldSet() {


		val Names = object : TableDefinition("Names") {
			val firstName = varchar("firstname", 10).primaryKey()
			val lastName = varchar("lastname", 10)
			val middleName = varchar("middleName", 10)
		}

		withTables(Names) {

			insertInto(Names).values {
				it[firstName] = "Foo"
				it[lastName] = "Bar"
				it[middleName] = "Or"
			}.execute()

			val updates = insertInto(Names).values {
				it[firstName] = "Foo"
				it[lastName] = "Bar2"
				it[middleName] = "Or2"
			}.onConflict(Names.firstName).update {
				it[lastName]
			}

			connection.dialect.statementSQL(updates).assertSQL {
				"INSERT INTO \"Names\" (firstname, lastname, middleName) VALUES (?, ?, ?) " +
						"ON CONFLICT (firstname) DO UPDATE SET lastname = excluded.lastname;"
			}

			updates.execute()

			val rows = from(Names).execute().toList()


			assertEquals(1, rows.size)
			assertEquals("Foo", rows[0][Names.firstName])
			assertEquals("Bar2", rows[0][Names.lastName])
			assertEquals("Or", rows[0][Names.middleName])
		}
	}

	@Test
	fun insertOnConflictAllFieldSet() {


		val Names = object : TableDefinition("Names") {
			val firstName = varchar("firstname", 10).primaryKey()
			val lastName = varchar("lastname", 10)
			val middleName = varchar("middleName", 10)
		}

		withTables(Names) {

			insertInto(Names).values {
				it[firstName] = "Foo"
				it[lastName] = "Bar"
				it[middleName] = "Or"
			}.execute()

			val updates = insertInto(Names).values {
				it[firstName] = "Foo"
				it[lastName] = "Bar2"
				it[middleName] = "Or2"
			}.onConflict(Names.firstName).update {
				it[lastName]
				it[middleName]
			}

			connection.dialect.statementSQL(updates).assertSQL {
				"INSERT INTO \"Names\" (firstname, lastname, middleName) VALUES (?, ?, ?) " +
						"ON CONFLICT (firstname) DO UPDATE SET lastname = excluded.lastname, middleName = excluded.middleName;"
			}

			updates.execute()

			val rows = from(Names).execute().toList()


			assertEquals(1, rows.size)
			assertEquals("Foo", rows[0][Names.firstName])
			assertEquals("Bar2", rows[0][Names.lastName])
			assertEquals("Or2", rows[0][Names.middleName])
		}
	}

	@Test
	fun insertWhenConflictDoNothing() {


		val Names = object : TableDefinition("Names") {
			val firstName = varchar("firstname", 10).primaryKey()
			val lastName = varchar("lastname", 10)
			val middleName = varchar("middleName", 10)
		}

		withTables(Names) {

			insertInto(Names).values {
				it[firstName] = "Foo"
				it[lastName] = "Bar"
				it[middleName] = "Or"
			}.execute()

			val updates = insertInto(Names).values {
				it[firstName] = "Foo"
				it[lastName] = "Bar2"
				it[middleName] = "Or2"
			}.onConflict(Names.firstName).doNothing()

			connection.dialect.statementSQL(updates).assertSQL {
				"INSERT INTO \"Names\" (firstname, lastname, middleName) VALUES (?, ?, ?) " +
						"ON CONFLICT (firstname) DO NOTHING;"
			}

			updates.execute()

			val rows = from(Names).execute().toList()

			assertEquals(1, rows.size)
			assertEquals("Foo", rows[0][Names.firstName])
			assertEquals("Bar", rows[0][Names.lastName])
			assertEquals("Or", rows[0][Names.middleName])
		}
	}

	@Test
	fun queryObject() {
		withCities {
			val query = query(Inhabitants)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name AS citizenName, Cities.name AS cityName FROM Citizens INNER JOIN Cities ON Cities.id = Citizens.city_id"
			}

			val rows = query.execute().toList().sortedBy { it[Inhabitants.citizenName] }
			assertEquals(3, rows.size)
			assertEquals("Andrey", rows[0][Inhabitants.citizenName])
			assertEquals("St. Petersburg", rows[0][Inhabitants.cityName])
			assertEquals("Eugene", rows[1][Inhabitants.citizenName])
			assertEquals("Munich", rows[1][Inhabitants.cityName])
			assertEquals("Sergey", rows[2][Inhabitants.citizenName])
			assertEquals("Munich", rows[2][Inhabitants.cityName])
		}
	}

	@Test
	fun queryObjectAltered() {
		withCities {
			// TODO: use { Inhabitants.citizenName eq "eugene" }, but WHERE doesn't work on alias
			val query = query(Inhabitants).where { Citizens.name eq "Eugene" }

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name AS citizenName, Cities.name AS cityName FROM Citizens INNER JOIN Cities ON Cities.id = Citizens.city_id WHERE Citizens.name = ?"
			}

			val rows = query.execute().toList()
			assertEquals(1, rows.size)
			assertEquals("Eugene", rows[0][Inhabitants.citizenName])
			assertEquals("Munich", rows[0][Inhabitants.cityName])
		}
	}

	@Test
	fun queryObjectAlteredTwice() {
		withTables {
			connection.dialect.statementSQL(
					query(Inhabitants).where { Inhabitants.citizenName eq "eugene" }).assertSQL {
				"SELECT Citizens.name AS citizenName, Cities.name AS cityName FROM Citizens INNER JOIN Cities ON Cities.id = Citizens.city_id WHERE citizenName = ?"
			}
			connection.dialect.statementSQL(query(Inhabitants).where { Inhabitants.cityName eq "Munchen" }).assertSQL {
				"SELECT Citizens.name AS citizenName, Cities.name AS cityName FROM Citizens INNER JOIN Cities ON Cities.id = Citizens.city_id WHERE cityName = ?"
			}
		}
	}

	@Test
	fun selectFromOrder() {
		withCities {
			val query = from(Citizens)
					.select(Citizens.name, Citizens.id)
					.orderBy(Citizens.name)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name, Citizens.id FROM Citizens ORDER BY ${nullsLast("Citizens.name")}"
			}

			val rows = query.execute().map { it[Citizens.name] }.toList()
			assertEquals(listOf("Alex", "Andrey", "Eugene", "Sergey", "Something"), rows)
		}
	}

	@Test
	fun selectFromOrderDescOrder() {
		withCities {
			val query = from(Citizens)
					.select(Citizens.name, Citizens.id, Citizens.cityId)
					.orderByDescending(Citizens.cityId)
					.orderBy(Citizens.name)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name, Citizens.id, Citizens.city_id FROM Citizens ORDER BY ${nullsLast(
						"Citizens.city_id DESC")}, ${nullsLast("Citizens.name")}"
			}

			val rows = query.execute().map { it[Citizens.name] }.toList()
			assertEquals(listOf("Eugene", "Sergey", "Andrey", "Alex", "Something"), rows)
		}
	}

	@Test
	fun selectFromOrderOrder() {
		withCities {
			val query = from(Citizens)
					.select(Citizens.name, Citizens.id, Citizens.cityId)
					.orderBy(Citizens.cityId)
					.orderBy(Citizens.name)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Citizens.name, Citizens.id, Citizens.city_id FROM Citizens ORDER BY ${nullsLast(
						"Citizens.city_id")}, ${nullsLast("Citizens.name")}"
			}

			// PG: NULLs are first, H2: NULLs are last
			val rows = query.execute().map { it[Citizens.name] }.toList()
			assertEquals(listOf("Andrey", "Eugene", "Sergey", "Alex", "Something"), rows)
		}
	}

	@Test
	fun selectCountGroupBy() {
		withCities {
			val query = from(Cities)
					.innerJoin(Citizens) { Cities.id eq Citizens.cityId }
					.select(Cities.name, Citizens.id.count().alias("citizens"))
					.groupBy(Cities.name)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Cities.name, COUNT(Citizens.id) AS citizens FROM Cities INNER JOIN Citizens ON Cities.id = Citizens.city_id GROUP BY Cities.name"
			}

			query.execute().forEach {
				val cityName = it[Cities.name]
				val userCount = it.get<Long>("citizens")

				when (cityName) {
					"Munich" -> assertEquals(2, userCount)
					"Prague" -> assertEquals(0, userCount)
					"St. Petersburg" -> assertEquals(1, userCount)
					else -> error("Unknown city $cityName")
				}
			}
		}
	}

	@Test
	fun selectMaxGroupByHaving() {
		withCities {
			val query = from(Cities)
					.innerJoin(Citizens) { Cities.id eq Citizens.cityId }
					.select(Cities.name, Citizens.id.max().alias("last_citizen"))
					.groupBy(Cities.name)
					.having { Citizens.id.count() gt 0 }

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Cities.name, MAX(Citizens.id) AS last_citizen FROM Cities INNER JOIN Citizens ON Cities.id = Citizens.city_id GROUP BY Cities.name HAVING COUNT(Citizens.id) > ?"
			}

			query.execute().forEach {
				val cityName = it[Cities.name]
				val lastCitizen = it.get<String>("last_citizen")

				when (cityName) {
					"Munich" -> assertEquals("sergey", lastCitizen)
					"St. Petersburg" -> assertEquals("andrey", lastCitizen)
					else -> error("Unmatching city $cityName")
				}
			}
		}
	}

	@Test
	fun selectCountGroupByHavingOrder() {
		withCities {
			val query = from(Cities)
					.innerJoin(Citizens) { Cities.id eq Citizens.cityId }
					.select(Cities.name, Citizens.id.max().alias("last_citizen"))
					.groupBy(Cities.name)
					.having { Citizens.id.count() gt 0 }
					.orderBy(Cities.name)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT Cities.name, MAX(Citizens.id) AS last_citizen FROM Cities INNER JOIN Citizens ON Cities.id = Citizens.city_id GROUP BY Cities.name HAVING COUNT(Citizens.id) > ? ORDER BY ${nullsLast(
						"Cities.name")}"
			}

			query.execute().forEachIndexed { index, row ->
				val cityName = row[Cities.name]
				val lastCitizen = row.get<String>("last_citizen")

				when (index) {
					0 -> {
						assertEquals("Munich", cityName)
						assertEquals("sergey", lastCitizen)
					}
					1 -> {
						assertEquals("St. Petersburg", cityName)
						assertEquals("andrey", lastCitizen)
					}
					else -> error("Wrong index $index")
				}
			}

		}
	}

	@Test
	fun selectFromNestedQuery() {
		withCities {
			val query = from(select(Citizens.name, Citizens.id).from(Citizens).alias("Citizens"))
					.orderBy(Citizens.name)

			connection.dialect.statementSQL(query).assertSQL {
				"SELECT * FROM (SELECT Citizens.name, Citizens.id FROM Citizens) AS Citizens ORDER BY ${nullsLast(
						"Citizens.name")}"
			}

			val rows = query.execute().map { it.get<String>("name") }.toList()
			assertEquals(listOf("Alex", "Andrey", "Eugene", "Sergey", "Something"), rows)
		}
	}
}
