package com.pli.formscanner.database

import android.content.Context
import kotlinx.coroutines.flow.Flow

class FormRepository(context: Context) {
    private val formDao = FormDatabase.getDatabase(context).formDao()

    fun getAllForms(): Flow<List<FormEntity>> = formDao.getAllForms()

    fun getFormsByStatus(completed: Boolean): Flow<List<FormEntity>> =
        formDao.getFormsByStatus(completed)

    suspend fun getFormById(formId: Long): FormEntity? = formDao.getFormById(formId)

    suspend fun insertForm(form: FormEntity): Long = formDao.insertForm(form)

    suspend fun updateForm(form: FormEntity) = formDao.updateForm(form)

    suspend fun deleteForm(form: FormEntity) = formDao.deleteForm(form)

    suspend fun deleteFormById(formId: Long) = formDao.deleteFormById(formId)

    suspend fun getFormsCount(): Int = formDao.getFormsCount()

    suspend fun saveFormData(fieldsMap: Map<String, String>, isCompleted: Boolean = false): Long {
        val formEntity = FormEntity(
            fieldsJson = com.google.gson.Gson().toJson(fieldsMap),
            isCompleted = isCompleted,
            timestamp = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        return insertForm(formEntity)
    }
}
