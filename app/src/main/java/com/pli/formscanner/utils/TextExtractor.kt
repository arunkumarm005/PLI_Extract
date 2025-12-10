package com.pli.formscanner.utils

import android.content.Context
import android.util.Log
import com.pli.formscanner.models.ExtractedField

/**
 * Enhanced text extractor that uses document-specific extraction logic
 */
class TextExtractor(private val context: Context) {

    private val TAG = "TextExtractor"

    fun extractFields(text: String): List<ExtractedField> {
        Log.d(TAG, "=== Starting field extraction ===")
        Log.d(TAG, "Raw OCR text length: ${text.length}")
        
        // Split into lines for better context analysis
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        Log.d(TAG, "Text lines (${lines.size}):")
        lines.forEachIndexed { index, line ->
            Log.d(TAG, "  Line $index: $line")
        }
        
        // Detect document type
        val documentType = DocumentDetector.detectDocumentType(text)
        Log.d(TAG, "Detected document type: $documentType")
        
        // Use document-specific extractor (ADVANCED VERSION for photocopies)
        val fields = when (documentType) {
            DocumentDetector.DocumentType.AADHAAR -> {
                Log.d(TAG, "Using ADVANCED Aadhaar extractor v4.0 (Photocopy optimized)")
                try {
                    AdvancedAadhaarExtractor.extractFields(text, lines)
                } catch (e: Exception) {
                    Log.e(TAG, "Advanced extractor failed, fallback to improved", e)
                    try {
                        ImprovedAadhaarExtractor.extractFields(text, lines)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Improved extractor failed, fallback to basic", e2)
                        AadhaarExtractor.extractFields(text, lines)
                    }
                }
            }
            DocumentDetector.DocumentType.PAN -> {
                Log.d(TAG, "Using IMPROVED PAN extractor")
                try {
                    ImprovedPANExtractor.extractFields(text, lines)
                } catch (e: Exception) {
                    Log.e(TAG, "Improved PAN extractor failed, fallback to old", e)
                    PANExtractor.extractFields(text, lines)
                }
            }
            DocumentDetector.DocumentType.UNKNOWN -> {
                Log.d(TAG, "Document type unknown, using generic extractor")
                extractGenericFields(text)
            }
        }
        
        Log.d(TAG, "Extracted ${fields.size} fields:")
        fields.forEach { field ->
            Log.d(TAG, "  - ${field.fieldLabel}: ${field.value} (${field.confidence}%)")
        }
        
        return fields
    }
    
    /**
     * Generic field extraction for non-specific documents
     * Fallback when document type cannot be determined
     */
    private fun extractGenericFields(text: String): List<ExtractedField> {
        val fields = mutableListOf<ExtractedField>()
        
        // Extract Aadhaar if present
        extractAadhaarNumber(text)?.let { fields.add(it) }
        
        // Extract PAN if present
        extractPANNumber(text)?.let { fields.add(it) }
        
        // Extract mobile number
        extractMobileNumber(text)?.let { fields.add(it) }
        
        // Extract email
        extractEmail(text)?.let { fields.add(it) }
        
        // Extract PIN code
        extractPINCode(text)?.let { fields.add(it) }
        
        // Extract date
        extractDate(text)?.let { fields.add(it) }
        
        return fields
    }
    
    private fun extractAadhaarNumber(text: String): ExtractedField? {
        val pattern = Regex("""(\d{4})[\s\-]?(\d{4})[\s\-]?(\d{4})""")
        val matches = pattern.findAll(text).toList()
        
        for (match in matches) {
            val number = match.groupValues[1] + match.groupValues[2] + match.groupValues[3]
            if (number.length == 12 && !number.all { it == '0' }) {
                return ExtractedField(
                    fieldName = "adharId",
                    fieldLabel = "Aadhaar Number",
                    value = "${number.substring(0, 4)} ${number.substring(4, 8)} ${number.substring(8, 12)}",
                    confidence = 85
                )
            }
        }
        return null
    }
    
    private fun extractPANNumber(text: String): ExtractedField? {
        val pattern = Regex("""([A-Z]{5}\d{4}[A-Z])""", RegexOption.IGNORE_CASE)
        val match = pattern.find(text)
        
        if (match != null) {
            val pan = match.value.uppercase()
            return ExtractedField(
                fieldName = "panNumber",
                fieldLabel = "PAN Number",
                value = pan,
                confidence = 85
            )
        }
        return null
    }
    
    private fun extractMobileNumber(text: String): ExtractedField? {
        val pattern = Regex("""(?:^|\s)([6-9]\d{9})(?:\s|$)""")
        val match = pattern.find(text)
        
        if (match != null) {
            val mobile = match.groupValues[1]
            return ExtractedField(
                fieldName = "mobileNumber",
                fieldLabel = "Mobile Number",
                value = mobile,
                confidence = 80
            )
        }
        return null
    }
    
    private fun extractEmail(text: String): ExtractedField? {
        val pattern = Regex("""([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})""")
        val match = pattern.find(text)
        
        if (match != null) {
            return ExtractedField(
                fieldName = "emailAddress",
                fieldLabel = "Email Address",
                value = match.value,
                confidence = 90
            )
        }
        return null
    }
    
    private fun extractPINCode(text: String): ExtractedField? {
        val pattern = Regex("""(?:^|\s)(\d{6})(?:\s|$)""")
        val matches = pattern.findAll(text).toList()
        
        for (match in matches) {
            val pin = match.groupValues[1]
            // Avoid matching with parts of Aadhaar or dates
            if (!pin.startsWith("19") && !pin.startsWith("20")) {
                return ExtractedField(
                    fieldName = "zip",
                    fieldLabel = "PIN Code",
                    value = pin,
                    confidence = 75
                )
            }
        }
        return null
    }
    
    private fun extractDate(text: String): ExtractedField? {
        val pattern = Regex("""(\d{2}[/-]\d{2}[/-]\d{4})""")
        val match = pattern.find(text)
        
        if (match != null) {
            val date = match.value
            val yearMatch = Regex("""\d{4}""").find(date)
            if (yearMatch != null) {
                val year = yearMatch.value.toInt()
                if (year in 1920..2024) {
                    return ExtractedField(
                        fieldName = "birthdate",
                        fieldLabel = "Date of Birth",
                        value = date,
                        confidence = 75
                    )
                }
            }
        }
        return null
    }
}
