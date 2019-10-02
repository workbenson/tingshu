package com.github.eprendre.tingshu.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.eprendre.tingshu.utils.Book

@Database(entities = [Book::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE my_books ADD COLUMN skipBeginning INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("ALTER TABLE my_books ADD COLUMN skipEnd INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("ALTER TABLE my_books ADD COLUMN isFree INTEGER DEFAULT 1 NOT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "book-db")
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
        }
    }
}