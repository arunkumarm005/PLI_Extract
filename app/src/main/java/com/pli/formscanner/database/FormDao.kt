package com.pli.formscanner.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FormDao {
    @Query("SELECT * FROM forms ORDER BY timestamp DESC")
    fun getAllForms(): Flow<List<FormEntity>>

    @Query("SELECT * FROM forms WHERE id = :formId")
    suspend fun getFormById(formId: Long): FormEntity?

    @Query("SELECT * FROM forms WHERE isCompleted = :completed ORDER BY timestamp DESC")
    fun getFormsByStatus(completed: Boolean): Flow<List<FormEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForm(form: FormEntity): Long

    @Update
    suspend fun updateForm(form: FormEntity)

    @Delete
    suspend fun deleteForm(form: FormEntity)

    @Query("DELETE FROM forms WHERE id = :formId")
    suspend fun deleteFormById(formId: Long)

    @Query("SELECT COUNT(*) FROM forms")
    suspend fun getFormsCount(): Int
}
