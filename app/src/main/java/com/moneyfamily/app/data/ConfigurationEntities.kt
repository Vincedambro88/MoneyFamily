package com.moneyfamily.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "types", indices = [Index(value = ["name"], unique = true)])
data class TypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = true
)

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = true
)

@Entity(tableName = "family_members", indices = [Index(value = ["name"], unique = true)])
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = true
)

@Entity(tableName = "type_category", indices = [Index(value = ["typeId"], unique = true)])
data class TypeCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val typeId: Long,
    val categoryId: Long
)
