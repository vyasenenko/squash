package org.jetbrains.squash.dialects.postgres.tests

import org.jetbrains.squash.tests.DatabaseModificationTest
import org.jetbrains.squash.tests.DatabaseTests

class PgDataBaseModificationTest : DatabaseModificationTest(), DatabaseTests by PgDatabaseTests()