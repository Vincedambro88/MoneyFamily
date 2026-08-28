package com.moneyfamily.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS types (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_types_name ON types(name)")
        db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories(name)")
        db.execSQL("CREATE TABLE IF NOT EXISTS family_members (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_family_members_name ON family_members(name)")
        db.execSQL("CREATE TABLE IF NOT EXISTS type_category (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, typeId INTEGER NOT NULL, categoryId INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_type_category_typeId ON type_category(typeId)")
    }
}
