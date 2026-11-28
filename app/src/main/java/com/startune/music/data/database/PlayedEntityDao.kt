package com.startune.music.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayedEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(entity: PlayedEntityEntity)

    @Query("SELECT * FROM played_entities ORDER BY played_at DESC LIMIT :limit")
    fun observeRecentPlayed(limit: Int): Flow<List<PlayedEntityEntity>>

    @Query("DELETE FROM played_entities")
    suspend fun clearAll()
}
