package org.jetbrains.squash.dialects.postgres.tests

import org.jetbrains.squash.tests.ChangeLogTest
import org.jetbrains.squash.tests.DatabaseTests

class PgChangeLogTest : ChangeLogTest(), DatabaseTests by PgDatabaseTests()