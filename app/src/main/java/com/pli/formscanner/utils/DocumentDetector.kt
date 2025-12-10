package com.pli.formscanner.utils

import android.util.Log

object DocumentDetector {

    enum class DocumentType {
        AADHAAR,
        PAN,
        UNKNOWN
    }

    private val aadhaarKeywords = listOf(
        "aadhaar", "aadhar", "आधार", "government of india",
        "unique identification", "uidai", "uid", "gov", "india"
    )

    private val panKeywords = listOf(
        "income tax", "permanent account number", "pan",
        "govt. of india", "government of india", "income tax department"
    )

    fun detectDocumentType(text: String): DocumentType {
        val lowerText = text.lowercase()
        var aadhaarScore = 0
        var panScore = 0

        // Keyword scoring
        aadhaarKeywords.forEach { keyword ->
            if (lowerText.contains(keyword)) {
                aadhaarScore += when (keyword) {
                    "aadhaar", "aadhar", "आधार" -> 10
                    "uidai", "unique identification" -> 8
                    else -> 3
                }
            }
        }

        panKeywords.forEach { keyword ->
            if (lowerText.contains(keyword)) {
                panScore += when (keyword) {
                    "permanent account number", "income tax department" -> 10
                    "pan" -> 8
                    else -> 3
                }
            }
        }

        // Pattern-based detection
        if (Regex("""\d{4}\s*\d{4}\s*\d{4}""").containsMatchIn(text)) {
            aadhaarScore += 5
        }
        if (Regex("""[A-Z]{5}\d{4}[A-Z]""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            panScore += 5
        }

        Log.d("DocumentDetector", "Aadhaar score: $aadhaarScore, PAN score: $panScore")

        // Lower threshold for Aadhaar detection
        return when {
            aadhaarScore >= 5 && aadhaarScore > panScore -> DocumentType.AADHAAR
            panScore >= 10 && panScore > aadhaarScore -> DocumentType.PAN
            else -> DocumentType.UNKNOWN
        }
    }
}
