package com.houvven.guise.log

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RuntimeLogDao {

    @Query("SELECT * FROM runtime_log ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<RuntimeLog>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(logs: List<RuntimeLog>)

    @Query("DELETE FROM runtime_log")
    suspend fun clearAll()

    @Query(
        """
        DELETE FROM runtime_log
        WHERE id NOT IN (
            SELECT id FROM runtime_log
            ORDER BY timestamp DESC, id DESC
            LIMIT :maximumCount
        )
        """
    )
    suspend fun trimTo(maximumCount: Int)
}
