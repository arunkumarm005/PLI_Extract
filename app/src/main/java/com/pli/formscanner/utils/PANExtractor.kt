package com.pli.formscanner.utils

import android.util.Log
import com.pli.formscanner.models.ExtractedField

/**
 * Specialized extractor for PAN cards
 * Handles the specific layout and structure of PAN documents
 */
object PANExtractor {
    
    private const val TAG = "PANExtractor"
    
    // Blacklisted texts that should NOT be considered as names
    private val nameBlacklist = setOf(
        "income tax",
        "income tax department",
        "govt of india",
        "government of india",
        "government",
        "permanent account number",
        "signature",
        "fathers name",
        "father's name",
        "india"
    )
    
    fun extractFields(text: String, lines: List<String>): List<ExtractedField> {
        val fields = mutableListOf<ExtractedField>()
        
        Log.d(TAG, "Extracting from PAN card")
        Log.d(TAG, "Text lines: ${lines.joinToString(" | ")}")
        
        // Extract PAN number
        extractPANNumber(text)?.let { fields.add(it) }
        
        // Extract name
        extractName(text, lines)?.let { fields.add(it) }
        
        // Extract father's name
        extractFatherName(text, lines)?.let { fields.add(it) }
        
        // Extract DOB
        extractDateOfBirth(text)?.let { fields.add(it) }
        
        return fields
    }
    
    private fun extractPANNumber(text: String): ExtractedField? {
        // PAN Format: 5 letters + 4 digits + 1 letter
        // Example: ABCDE1234F
        val pattern = Regex("""([A-Z]{5}\d{4}[A-Z])""", RegexOption.IGNORE_CASE)
        
        val matches = pattern.findAll(text).toList()
        
        for (match in matches) {
            val panNumber = match.value.uppercase()
            
            // Validate PAN structure
            if (isValidPAN(panNumber)) {
                Log.d(TAG, "PAN number found: $panNumber")
                
                return ExtractedField(
                    fieldName = "panNumber",
                    fieldLabel = "PAN Number",
                    value = panNumber,
                    confidence = 95
                )
            }
        }
        
        return null
    }
    
    private fun isValidPAN(pan: String): Boolean {
        // PAN validation rules:
        // - 4th character should be 'P' for individual
        // - Should not be a common word
        if (pan.length != 10) return false
        
        val fourthChar = pan[3]
        // Accept P (individual), C (company), H (HUF), F (firm), etc.
        if (fourthChar !in setOf('P', 'C', 'H', 'F', 'A', 'T', 'B', 'L', 'J', 'G')) {
            return false
        }
        
        return true
    }
    
    private fun extractName(text: String, lines: List<String>): ExtractedField? {
        // Strategy 1: Look for "Name" label
        val nameLabelPatterns = listOf(
            Regex("""(?:name)\s*:?\s*([A-Z][A-Z\s]+)""", RegexOption.IGNORE_CASE),
            Regex("""([A-Z][A-Z\s]{5,40})""") // Fallback: all caps name (common in PAN cards)
        )
        
        for (pattern in nameLabelPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (isValidName(name, isPANFormat = true)) {
                    Log.d(TAG, "Name found: $name")
                    return createNameField(name, 85)
                }
            }
        }
        
        // Strategy 2: Find name in specific position
        // PAN cards usually have name prominently displayed in ALL CAPS
        for (line in lines) {
            val trimmedLine = line.trim()
            
            // Skip lines with blacklisted terms
            val lowerLine = trimmedLine.lowercase()
            if (nameBlacklist.any { lowerLine.contains(it) }) {
                continue
            }
            
            // Skip lines with PAN number
            if (Regex("""[A-Z]{5}\d{4}[A-Z]""").containsMatchIn(trimmedLine)) {
                continue
            }
            
            // Look for all-caps names (typical PAN format)
            if (trimmedLine.length in 5..40 && 
                trimmedLine == trimmedLine.uppercase() &&
                trimmedLine.all { it.isLetter() || it.isWhitespace() }) {
                
                if (isValidName(trimmedLine, isPANFormat = true)) {
                    Log.d(TAG, "Name found from line: $trimmedLine")
                    return createNameField(trimmedLine, 80)
                }
            }
        }
        
        return null
    }
    
    private fun extractFatherName(text: String, lines: List<String>): ExtractedField? {
        // Look for father's name after label
        val patterns = listOf(
            Regex("""father'?s?\s+name\s*:?\s*([A-Z][A-Z\s]+)""", RegexOption.IGNORE_CASE),
            Regex("""father\s*:?\s*([A-Z][A-Z\s]+)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val fatherName = match.groupValues[1].trim()
                if (isValidName(fatherName, isPANFormat = true)) {
                    Log.d(TAG, "Father's name found: $fatherName")
                    return ExtractedField(
                        fieldName = "fatherName",
                        fieldLabel = "Father's Name",
                        value = fatherName,
                        confidence = 85
                    )
                }
            }
        }
        
        return null
    }
    
    private fun isValidName(name: String, isPANFormat: Boolean = false): Boolean {
        val lowerName = name.lowercase()
        val trimmedName = name.trim()
        
        // Must have at least 2 characters
        if (trimmedName.length < 2) return false
        
        // Check against blacklist
        if (nameBlacklist.any { lowerName.contains(it) }) {
            return false
        }
        
        // Should not contain numbers
        if (name.any { it.isDigit() }) {
            return false
        }
        
        // For PAN format (all caps), check word count
        if (isPANFormat) {
            val words = trimmedName.split(Regex("""\s+""")).filter { it.isNotEmpty() }
            // PAN names usually have 2-4 words
            if (words.size !in 2..4) return false
            
            // Each word should be reasonable length
            if (words.any { it.length < 2 || it.length > 20 }) {
                return false
            }
        }
        
        return true
    }
    
    private fun createNameField(name: String, confidence: Int): ExtractedField {
        // Convert ALL CAPS to Title Case for better readability
        val formattedName = name.split(Regex("""\s+"""))
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
        
        return ExtractedField(
            fieldName = "firstName",
            fieldLabel = "Full Name",
            value = formattedName,
            confidence = confidence
        )
    }
    
    private fun extractDateOfBirth(text: String): ExtractedField? {
        // Common DOB patterns in PAN
        val patterns = listOf(
            Regex("""(?:dob|date of birth)\s*:?\s*(\d{2}[/-]\d{2}[/-]\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{2}[/-]\d{2}[/-]\d{4})""")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val dobValue = match.groupValues[1]
                
                // Validate year range
                val yearMatch = Regex("""\d{4}""").find(dobValue)
                if (yearMatch != null) {
                    val year = yearMatch.value.toInt()
                    if (year in 1920..2024) {
                        Log.d(TAG, "DOB found: $dobValue")
                        return ExtractedField(
                            fieldName = "birthdate",
                            fieldLabel = "Date of Birth",
                            value = dobValue,
                            confidence = 85
                        )
                    }
                }
            }
        }
        
        return null
    }
}
