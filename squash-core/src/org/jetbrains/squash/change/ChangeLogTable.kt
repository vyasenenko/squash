package org.jetbrains.squash.change

import org.jetbrains.squash.definition.*

/**
 * Change log table definition.
 * *
 * @author Vitaliy Yasenenko.
 */
internal object ChangeLogTable : TableDefinition("database_change_log") {
    val vid = integer("vid")
    val name = varchar("name", 20)
    val query = text("query")
    val whenChanged = datetime("when").now()

    init {
        primaryKey(vid, name)
    }
}