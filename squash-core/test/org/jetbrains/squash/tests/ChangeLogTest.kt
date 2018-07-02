package org.jetbrains.squash.tests

import org.jetbrains.squash.change.ChangeLogController
import org.jetbrains.squash.change.ChangeLogStatement
import org.jetbrains.squash.change.ChangedData
import org.jetbrains.squash.tests.data.TestChangeLog
import org.jetbrains.squash.tests.data.TestChangeLogAppendQuery
import org.jetbrains.squash.tests.data.TestChangeLogIllegal
import org.jetbrains.squash.tests.data.TestChangeLogNotValid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Check change log database system.
 * *
 * @author Vitaliy Yasenenko.
 */
abstract class ChangeLogTest : DatabaseTests {

    @Test
    fun shouldChangeLogExecuteAndWriteChangesInDb(): Unit = withTransaction {
        val sut = databaseSchema().changeLogController

        val checker = CheckQuery(this)

        sut.executeAndCheck(TestChangeLog, 3) // Execute change log

        assertEquals(8, checker.countQueries)
        // [2] query selected all changelog before and after, [3] execute changes, [3] insert changes

        val allChangeLog = sut.executeAndCheck(TestChangeLog, 3) // Again execute (expect not change size)

        assertEquals(10, checker.countQueries)
        // append [2] query selected all changelog before and after

        checkResult(TestChangeLog.list, allChangeLog)
    }

    @Test
    fun shouldChangeLogExecuteAndAppendNewQuery(): Unit = withTransaction {
        val sut = databaseSchema().changeLogController

        val checker = CheckQuery(this)

        sut.executeAndCheck(TestChangeLog, 3) // Execute change log

        assertEquals(8, checker.countQueries)
        // [2] query selected all changelog before and after, [3] execute changes, [3] insert changes

        val allChangeLog = sut.executeAndCheck(TestChangeLogAppendQuery, 4) // Append new query

        assertEquals(12, checker.countQueries)
        // append [2] query selected all changelog before and after, [1] execute changes, [1] insert changes

        checkResult(TestChangeLog.list, allChangeLog)
    }

    @Test(expected = IllegalStateException::class)
    fun expectedErrorChangeQuery(): Unit = withTransaction {
        val sut = databaseSchema().changeLogController

        val checker = CheckQuery(this)

        sut.execute(TestChangeLog)

        assertEquals(8, checker.countQueries)
        // [2] query selected all changelog before and after, [3] execute changes, [3] insert changes

        sut.execute(TestChangeLogIllegal) // Try execute not equals query
    }

    @Test(expected = Exception::class)
    fun shouldNotValidExecutedQuery(): Unit = withTransaction {
        val sut = databaseSchema().changeLogController

        sut.execute(TestChangeLogNotValid)

        assert(true)
    }

    private fun ChangeLogController.executeAndCheck(statement: ChangeLogStatement, expectedSize: Int) =
            execute(statement).apply {
                assertEquals(expectedSize, size)
            }

    private fun checkResult(list: List<ChangeLogStatement.ChangingExecutedStatement>, allExecuted: List<ChangedData>) {
        list.forEach { executeStatement ->
            assertNotNull(allExecuted.find {
                it.vid == executeStatement.vid
                        && it.name == TestChangeLog.nameChangeLog
                        && executeStatement.query == it.query
            })
        }
    }
}

