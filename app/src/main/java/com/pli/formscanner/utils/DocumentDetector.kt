package com.pli.formscanner.utils

import android.util.Log

/**
 * Detects the type of document being scanned based on OCR text
 */
object DocumentDetector {
    
    enum class DocumentType {
        AADHAAR,
        PAN,
        UNKNOWN
    }
    
    private val aadhaarKeywords = listOf(
        "aadhaar",
        "aadhar", 
        "आधार",
        "government of india",
        "unique identification",
        "uidai",
        "uid"
    )
    
    private val panKeywords = listOf(
        "income tax",
        "permanent account number",
        "pan",
        "govt. of india",
        "government of india",
        "income tax department"
    )
    
    fun detectDocumentType(text: String): DocumentType {
        val lowerText = text.lowercase()
        
        var aadhaarScore = 0
        var panScore = 0
        
        // Count keyword matches
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
        // Aadhaar: 12-digit pattern
        if (Regex("""\d{4}\s*\d{4}\s*\d{4}""").containsMatchIn(text)) {
            aadhaarScore += 5
        }
        
        // PAN: 5 letters + 4 digits + 1 letter pattern
        if (Regex("""[A-Z]{5}\d{4}[A-Z]""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            panScore += 5
        }
        
        Log.d("DocumentDetector", "Aadhaar score: $aadhaarScore, PAN score: $panScore")
        
        return when {
            aadhaarScore > panScore && aadhaarScore >= 10 -> DocumentType.AADHAAR
            panScore > aadhaarScore && panScore >= 10 -> DocumentType.PAN
            else -> DocumentType.UNKNOWN
        }
    }
}
