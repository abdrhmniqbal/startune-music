package com.startune.music.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * One row per played entity: the last time the user played FROM an album, artist,
 * playlist, or mix. Replaying the same entity replaces its row, so the table holds
 * at most one row per entity and ordering by [playedAt] is the Jump back in order.
 * The [entityName] is the name at record time; the live name wins at read time when
 * the entity still exists, and the recorded name is the fallback for entities that
 * no longer resolve (the built-in For You and Daily mixes have no library row).
 */
@Entity(
    tableName = "played_entities",
    primaryKeys = ["entity_type", "entity_id"]
)
data class PlayedEntityEntity(
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "entity_name")
    val entityName: String,
    @ColumnInfo(name = "played_at")
    val playedAt: Long
)
