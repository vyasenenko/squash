package org.jetbrains.squash.dialects.postgres.tests

import org.jetbrains.squash.tests.DataBaseModificationTest
import org.jetbrains.squash.tests.DatabaseTests

class PgDataBaseModificationTest : DataBaseModificationTest(), DatabaseTests by PgDatabaseTests()