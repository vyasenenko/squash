package org.jetbrains.squash.statements

import org.jetbrains.squash.definition.Column
import org.jetbrains.squash.definition.Table


open class ConflictStatement<T : Table, R>(val insertValuesStatement: InsertValuesStatement<T, R>,
                                           val conflictColumns: List<Column<*>>) : Statement<R> {

	val table: T
		get() = insertValuesStatement.table

	val updateColumn: List<Column<*>>
		get() = updater.updateColumn

	private var updater: Updater = Updater()

	fun update(func: T.(Updater) -> Unit): ConflictStatement<T, R> {
		func(table, updater)
		return this
	}

	fun doNothing(): ConflictStatement<T, R> {
		updater = Updater()
		return this
	}

	fun update(column: Column<*>): ConflictStatement<T, R> {
		updater[column]
		return this
	}
}

class Updater {
	private val mutableList: MutableList<Column<*>> = mutableListOf()

	val updateColumn: List<Column<*>>
		get() = mutableList

	operator fun get(column: Column<*>){
		if (!mutableList.contains(column)) {
			mutableList.add(column)
		}
	}
}


fun <T : Table, R> InsertValuesStatement<T, R>.onConflict(vararg columns: Column<*>): ConflictStatement<T, R> {
	return ConflictStatement(this, columns.toList())
}
