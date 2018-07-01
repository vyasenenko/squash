package org.jetbrains.squash.tests

import org.jetbrains.squash.change.ChangeLogStatement
import org.jetbrains.squash.change.ChangedData
import org.jetbrains.squash.tests.data.TestChangeLog
import org.jetbrains.squash.tests.data.TestChangeLogAppendQuery
import org.jetbrains.squash.tests.data.TestChangeLogIllegal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Check change log database system.
 * *
 * @author Vitaliy Yasenenko.
 */
abstract class ChangeLogTest : DatabaseTests {

    @AfterTest
    fun after() = withTransaction {
        databaseSchema().changeLogController.dropChangeLogTable()
    }

    @Test
    fun shouldChangeLogExecuteAndWriteChangesInDb(): Unit = withTransaction {
        val sut = databaseSchema().changeLogController

        fun allExecuted(expectedSize: Int) = sut.getAllChanges().apply {
            assertEquals(expectedSize, size)
        }

        sut.executeChangeLog(TestChangeLog) // Execute change log

        allExecuted(3)

        sut.executeChangeLog(TestChangeLog) // Again execute (expect not change size)

        checkResult(TestChangeLog.list, allExecuted(3))
    }

    @Test
    fun shouldChangeLogExecuteAndAppendNewQuery(): Unit = withTransaction {
        val sut = databaseSchema().changeLogController

        fun allExecuted(expectedSize: Int) = sut.getAllChanges().apply {
            assertEquals(expectedSize, size)
        }

        sut.executeChangeLog(TestChangeLog) // Execute change log

        allExecuted(3)

        sut.executeChangeLog(TestChangeLogAppendQuery) // Append new query

        checkResult(TestChangeLog.list, allExecuted(4))
    }

    @Test(expected = IllegalStateException::class)
    fun expectedErrorChangeQuery(): Unit = withTransaction {
        val sut = databaseSchema().changeLogController
        sut.executeChangeLog(TestChangeLog)
        sut.executeChangeLog(TestChangeLogIllegal) // Try execute not equals query
    }

    private fun checkResult(list: MutableList<ChangeLogStatement.ChangingExecutedStatement>, allExecuted: List<ChangedData>) {
        list.forEach { executeStatement ->
            assertNotNull(allExecuted.find {
                it.vid == executeStatement.vid
                        && it.name == TestChangeLog.nameChangeLog
                        && executeStatement.query == it.query
            })
        }
    }
}

