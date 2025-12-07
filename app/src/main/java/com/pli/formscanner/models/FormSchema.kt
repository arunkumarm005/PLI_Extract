package com.pli.formscanner.models

data class FormSchema(
    val properties: Map<String, SectionSchema>
)

data class SectionSchema(
    val title: String,
    val properties: Map<String, FieldSchema>
)

data class FieldSchema(
    val fieldLabel: String,
    val type: String,
    val pattern: String? = null,
    val required: Boolean = false,
    val maxLength: Int? = null,
    val minLength: Int? = null
)