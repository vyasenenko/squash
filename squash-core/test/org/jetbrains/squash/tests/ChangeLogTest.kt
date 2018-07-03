package org.jetbrains.squash.tests

import org.jetbrains.squash.change.ChangeLogController
import org.jetbrains.squash.change.ChangeLogStatement
import org.jetbrains.squash.change.ChangedData
import org.jetbrains.squash.tests.data.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Check change log database system.
 * *
 * @author Vitaliy Yasenenko.
 */
abstract class ChangeLogTest : DatabaseTests {

    @Test
    fun shouldChangeLogExecuteAndWriteChangesInDb() = withChangeLog { SUT ->

        val checker = CheckQuery(this)

        SUT.executeAndCheck(TestChangeLog, 3) // Execute change log

        checker.assert(8, "[1,8] query selected all changelog", "[2,4,6] execute changes", "[3,5,7] insert changes")

        val allChangeLog = SUT.executeAndCheck(TestChangeLog, 3) // Again execute (expect not change size)

        checker.assert(10, "[9, 10] query selected all changelog")

        checkResult(TestChangeLog.list, allChangeLog)
    }

    @Test
    fun shouldChangeLogExecuteAndAppendNewQuery() = withChangeLog { SUT ->

        val checker = CheckQuery(this)

        SUT.executeAndCheck(TestChangeLog, 3) // Execute change log

        checker.assert(8, "[1, 8] query selected all changelog", "[2, 4, 6] execute changes", "[3, 5, 7] insert changes")

        val allChangeLog = SUT.executeAndCheck(TestChangeLogAppendQuery, 4) // Append new query

        checker.assert(12, "[9, 12] query selected all changelog", "[10] execute changes", "[11] insert changes")

        checkResult(TestChangeLog.list, allChangeLog)
    }

    @Test(expected = IllegalStateException::class)
    fun expectedErrorChangeQuery() = withChangeLog { SUT ->

        val checker = CheckQuery(this)

        SUT.execute(TestChangeLog)

        checker.assert(8, "[1, 8] query selected all changelog", "[2, 4, 6] execute changes", "[3, 5, 7] insert changes")

        val list = SUT.execute(TestChangeLogIllegal) // Try execute not equals query

        assertTrue(list.isEmpty())
    }

    @Test(expected = Exception::class)
    fun shouldNotValidExecutedQuery() = withChangeLog { SUT ->

        SUT.execute(TestChangeLogNotValid)

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

