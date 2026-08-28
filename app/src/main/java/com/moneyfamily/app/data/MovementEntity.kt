package com.moneyfamily.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movements")
data class MovementEntity(
    @PrimaryKey val id: Long,
    val type: String,
    val typeName: String = "",
    val amount: Double,
    val category: String,
    val description: String,
    val date: String,
    val member: String,
    val paymentMethod: String
)
