package com.moneyfamily.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MovementDao {
    @Query("SELECT * FROM movements ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<MovementEntity>

    @Insert
    suspend fun insert(item: MovementEntity)

    @Update
    suspend fun update(item: MovementEntity)

    @Delete
    suspend fun delete(item: MovementEntity)
}
