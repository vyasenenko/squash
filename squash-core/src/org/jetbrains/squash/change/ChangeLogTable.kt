package org.jetbrains.squash.change

import org.jetbrains.squash.definition.*

/**
 * Change log table definition.
 * *
 * @author Vitaliy Yasenenko.
 */
internal object ChangeLogTable : TableDefinition("changelog") {
    val vid = integer("vid").primaryKey()
    val name = varchar("name", 20)
    val query = text("query")
    val whenChanged = datetime("when").now()

    init {
        primaryKey(vid, name)
    }
}