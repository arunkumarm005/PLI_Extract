package com.pli.formscanner.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExtractedField(
    val fieldName: String,
    val fieldLabel: String,
    val value: String,
    val confidence: Int,
    val section: String = ""
) : Parcelable