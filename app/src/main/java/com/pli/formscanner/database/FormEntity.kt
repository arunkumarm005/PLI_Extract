package com.pli.formscanner.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "forms")
data class FormEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val formType: String = "PLI",
    val fieldsJson: String, // JSON string of Map<String, String>
    val isCompleted: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromFieldsMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toFieldsMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type)
    }
}
