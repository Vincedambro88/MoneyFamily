package com.moneyfamily.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TypeDao {
    @Query("SELECT * FROM types ORDER BY name") suspend fun all(): List<TypeEntity>
    @Query("SELECT * FROM types WHERE active = 1 ORDER BY name") suspend fun active(): List<TypeEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(item: TypeEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<TypeEntity>)
    @Update suspend fun update(item: TypeEntity)
    @Query("UPDATE types SET active = :active WHERE id = :id") suspend fun setActive(id: Long, active: Boolean)
    @Query("DELETE FROM types") suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name") suspend fun all(): List<CategoryEntity>
    @Query("SELECT * FROM categories WHERE active = 1 ORDER BY name") suspend fun active(): List<CategoryEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(item: CategoryEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<CategoryEntity>)
    @Update suspend fun update(item: CategoryEntity)
    @Query("UPDATE categories SET active = :active WHERE id = :id") suspend fun setActive(id: Long, active: Boolean)
    @Query("DELETE FROM categories") suspend fun deleteAll()
}

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY name") suspend fun all(): List<FamilyMemberEntity>
    @Query("SELECT * FROM family_members WHERE active = 1 ORDER BY name") suspend fun active(): List<FamilyMemberEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(item: FamilyMemberEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<FamilyMemberEntity>)
    @Update suspend fun update(item: FamilyMemberEntity)
    @Query("UPDATE family_members SET active = :active WHERE id = :id") suspend fun setActive(id: Long, active: Boolean)
    @Query("DELETE FROM family_members") suspend fun deleteAll()
}

@Dao
interface TypeCategoryDao {
    @Query("SELECT * FROM type_category") suspend fun all(): List<TypeCategoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: TypeCategoryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<TypeCategoryEntity>)
    @Query("DELETE FROM type_category WHERE typeId = :typeId") suspend fun deleteForType(typeId: Long)
    @Query("DELETE FROM type_category") suspend fun deleteAll()
}
