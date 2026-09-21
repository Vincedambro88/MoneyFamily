package com.moneyfamily.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operation_tombstones")
data class OperationTombstone(
    @PrimaryKey val cloudId: String,
    val deletedAt: String
)
