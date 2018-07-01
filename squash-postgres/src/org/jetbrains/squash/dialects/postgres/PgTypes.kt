package org.jetbrains.squash.dialects.postgres

import org.jetbrains.squash.definition.*
import kotlin.reflect.KClass

enum class PgTypes(vararg kcss: KClass<out ColumnType>) : ColumnTypeDB {

    serial(IntColumnType::class),
    bigserial(LongColumnType::class),
    varchar(StringColumnType::class),
    bpchar(CharColumnType::class),
    int4(EnumColumnType::class, IntColumnType::class),
    numeric(DecimalColumnType::class),
    int8(LongColumnType::class),
    date(DateColumnType::class),
    bool(BooleanColumnType::class),
    timestamp(DateTimeColumnType::class),
    text(StringColumnType::class),
    bytea(BlobColumnType::class, BinaryColumnType::class),
    uuid(UUIDColumnType::class);

    override val classes: List<KClass<out ColumnType>> = kcss.toMutableList().apply {
        this.add(ReferenceColumnType::class)
    }
}