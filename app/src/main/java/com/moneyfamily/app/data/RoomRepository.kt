package com.moneyfamily.app.data

import android.content.Context
import androidx.room.Room

class RoomRepository(context: Context) {
    private val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "moneyfamily.db").fallbackToDestructiveMigration().build()
    private val dao = db.movementDao()

    suspend fun all(): List<Movement> = dao.getAll().map { it.toModel() }
    suspend fun insert(item: Movement) = dao.insert(item.toEntity())
    suspend fun update(item: Movement) = dao.update(item.toEntity())
    suspend fun delete(item: Movement) = dao.delete(item.toEntity())
}

private fun MovementEntity.toModel() = Movement(id, if (type == MovementType.INCOME.name) MovementType.INCOME else MovementType.EXPENSE, amount, category, description, date, member, paymentMethod)
private fun Movement.toEntity() = MovementEntity(id, type.name, amount, category, description, date, member, paymentMethod)
