package com.moneyfamily.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MovementEntity::class,
        TypeEntity::class,
        CategoryEntity::class,
        FamilyMemberEntity::class,
        TypeCategoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movementDao(): MovementDao
    abstract fun typeDao(): TypeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun typeCategoryDao(): TypeCategoryDao
}
