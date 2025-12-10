package com.pli.formscanner.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [FormEntity::class, ProposalEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class, ProposalConverters::class)
abstract class FormDatabase : RoomDatabase() {
    abstract fun formDao(): FormDao
    abstract fun proposalDao(): ProposalDao

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
