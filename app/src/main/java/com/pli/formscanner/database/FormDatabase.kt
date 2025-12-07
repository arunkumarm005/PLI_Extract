package com.pli.formscanner.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [FormEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FormDatabase : RoomDatabase() {
    abstract fun formDao(): FormDao

    companion object {
        @Volatile
        private var INSTANCE: FormDatabase? = null

        fun getDatabase(context: Context): FormDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FormDatabase::class.java,
                    "form_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
