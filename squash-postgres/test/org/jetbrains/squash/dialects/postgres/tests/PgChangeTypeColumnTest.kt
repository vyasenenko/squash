package org.jetbrains.squash.dialects.postgres.tests

import org.jetbrains.squash.tests.ChangeTypeColumnTest
import org.jetbrains.squash.tests.DatabaseModificationTest
import org.jetbrains.squash.tests.DatabaseTests

class PgChangeTypeColumnTest : ChangeTypeColumnTest(), DatabaseTests by PgDatabaseTests()