package org.jetbrains.squash.definition

import org.jetbrains.squash.query.*
import java.time.LocalDateTime

/**
 * Represents a column in a database [Table]
 *
 * Column is also a [NamedExpression] which allows it to be used in expressions.
 *
 * @param V type of the value in this column
 */
interface Column<out V> : NamedExpression<Name, V> {
    /**
     * [CompoundElement] to which this column belongs
     */
    val compound: CompoundElement

    /**
     * Database type of the column
     */
    val type: ColumnType

    /**
     * List of additional properties of the column, like autoincrement, nullable, etc
     */
    val properties: List<ColumnProperty>
}

inline fun <reified T : ColumnProperty> Column<*>.hasProperty(): Boolean {
    return properties.any { it is T }
}

inline fun <reified T : ColumnProperty> Column<*>.propertyOrNull(): T? {
    return properties.filterIsInstance<T>().singleOrNull()
}

open class ColumnDefinition<out V>(final override val compound: TableDefinition, name: Identifier, override val type: ColumnType) : Column<V> {
    override fun toString(): String = "$name: $type${properties.joinToString("")}"
    override val properties = mutableListOf<ColumnProperty>()
    override val name = QualifiedIdentifier<Name>(compound.compoundName, name)
}

class ReferenceColumn<out V>(compound: TableDefinition, name: Identifier, val reference: Column<V>) : ColumnDefinition<V>(compound, name, ReferenceColumnType(reference.type)) {
    override fun toString(): String = "&${name.id}\uD83D\uDD17$reference"
}

fun <C : ColumnDefinition<LocalDateTime>> C.now(): C = addProperty(NowTimeProperty())

fun <C : ColumnDefinition<*>> C.rename(oldName: String): C = addProperty(RenameColumnProperty(oldName))

class NowTimeProperty : ColumnProperty {
    override fun toString(): String = "=now()"
}

class RenameColumnProperty(val oldName: String) : ColumnProperty {
    override fun toString(): String = " renamed from '$oldName'"
}