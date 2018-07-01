package org.jetbrains.squash.tests.data

import org.jetbrains.squash.change.ChangeLogStatement

object TestChangeLog : ChangeLogStatement(name = "test_change_log", changing = {
    this["CREATE TABLE test_change_log_table (id SERIAL NOT NULL CONSTRAINT pk_test_change_log_table PRIMARY KEY, name VARCHAR(20) NOT NULL)"]
    this["ALTER TABLE test_change_log_table ADD COLUMN date TIMESTAMP"]
    this["DROP TABLE test_change_log_table"]
})

object TestChangeLogAppendQuery : ChangeLogStatement(name = "test_change_log", changing = {
    this["CREATE TABLE test_change_log_table (id SERIAL NOT NULL CONSTRAINT pk_test_change_log_table PRIMARY KEY, name VARCHAR(20) NOT NULL)"]
    this["ALTER TABLE test_change_log_table ADD COLUMN date TIMESTAMP"]
    this["DROP TABLE test_change_log_table"]
    this["SELECT t.* FROM changelog t LIMIT 502"]
})

object TestChangeLogIllegal : ChangeLogStatement(name = "test_change_log", changing = {
    this["NOT EQUALS QUERY"]
    this["ALTER TABLE test_change_log_table ADD COLUMN date TIMESTAMP"]
    this["DROP TABLE test_change_log_table"]
})