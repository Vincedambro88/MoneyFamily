package com.moneyfamily.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OperationTombstoneDao {
    @Query("SELECT * FROM operation_tombstones")
    suspend fun getAll(): List<OperationTombstone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: OperationTombstone)

    @Query("DELETE FROM operation_tombstones WHERE cloudId = :cloudId")
    suspend fun delete(cloudId: String)

    @Query("DELETE FROM operation_tombstones")
    suspend fun deleteAll()
}
