package com.pli.formscanner.utils

import android.util.Log
import com.pli.formscanner.models.ExtractedField

/**
 * Specialized extractor for Aadhaar cards
 * Handles the specific layout and structure of Aadhaar documents
 */
object AadhaarExtractor {
    
    private const val TAG = "AadhaarExtractor"
    
    // Blacklisted texts that should NOT be considered as names
    private val nameBlacklist = setOf(
        "government of india",
        "govt of india",
        "government",
        "govt",
        "india",
        "aadhaar",
        "aadhar",
        "male",
        "female",
        "dob",
        "year of birth",
        "unique identification authority",
        "uidai",
        "help",
        "आधार"
    )
    
    fun extractFields(text: String, lines: List<String>): List<ExtractedField> {
        val fields = mutableListOf<ExtractedField>()
        val lowerText = text.lowercase()
        
        Log.d(TAG, "Extracting from Aadhaar card")
        Log.d(TAG, "Text lines: ${lines.joinToString(" | ")}")
        
        // Extract Aadhaar number (12 digits)
        extractAadhaarNumber(text)?.let { fields.add(it) }
        
        // Extract name (context-aware)
        extractName(text, lines)?.let { fields.add(it) }
        
        // Extract DOB
        extractDateOfBirth(text)?.let { fields.add(it) }
        
        // Extract gender
        extractGender(text)?.let { 
            Log.d(TAG, "Gender extracted: ${it.value}")
        }
        
        return fields
    }
    
    private fun extractAadhaarNumber(text: String): ExtractedField? {
        // Pattern: 4 digits, space/hyphen, 4 digits, space/hyphen, 4 digits
        val pattern = Regex("""(\d{4})[\s\-]?(\d{4})[\s\-]?(\d{4})""")
        
        val matches = pattern.findAll(text).toList()
        
        for (match in matches) {
            val number = match.groupValues[1] + match.groupValues[2] + match.groupValues[3]
            
            // Validate: must be exactly 12 digits and not all zeros
            if (number.length == 12 && !number.all { it == '0' }) {
                Log.d(TAG, "Aadhaar number found: ${number.take(4)}********")
                
                return ExtractedField(
                    fieldName = "adharId",
                    fieldLabel = "Aadhaar Number",
                    value = "${number.substring(0, 4)} ${number.substring(4, 8)} ${number.substring(8, 12)}",
                    confidence = 95
                )
            }
        }
        
        return null
    }
    
    private fun extractName(text: String, lines: List<String>): ExtractedField? {
        val lowerText = text.lowercase()
        
        // Strategy 1: Find name after "name:" or similar labels
        val nameLabelPatterns = listOf(
            Regex("""(?:name|नाम)\s*:?\s*([A-Z][a-z]+(?:\s+[A-Z][a-z]+){1,3})""", RegexOption.IGNORE_CASE),
            Regex("""(?:name|नाम)\s*([A-Z][a-z]+(?:\s+[A-Z][a-z]+){1,3})""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in nameLabelPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (isValidName(name)) {
                    Log.d(TAG, "Name found via label: $name")
                    return createNameField(name, 90)
                }
            }
        }
        
        // Strategy 2: Find capitalized name in specific lines
        // Aadhaar cards typically have name in the upper portion
        val candidateLines = lines.take(minOf(10, lines.size))
        
        for (line in candidateLines) {
            // Skip lines with blacklisted terms
            val lowerLine = line.lowercase()
            if (nameBlacklist.any { lowerLine.contains(it) }) {
                continue
            }
            
            // Skip lines with numbers (likely not a name)
            if (Regex("""\d{3,}""").containsMatchIn(line)) {
                continue
            }
            
            // Look for capitalized words (name pattern)
            val namePattern = Regex("""([A-Z][a-z]+(?:\s+[A-Z][a-z]+){1,3})""")
            val match = namePattern.find(line)
            
            if (match != null) {
                val candidateName = match.value.trim()
                
                if (isValidName(candidateName)) {
                    Log.d(TAG, "Name candidate from line: $candidateName")
                    return createNameField(candidateName, 80)
                }
            }
        }
        
        Log.d(TAG, "No valid name found")
        return null
    }
    
    private fun isValidName(name: String): Boolean {
        val lowerName = name.lowercase()
        
        // Must have at least 2 words (first and last name)
        val words = name.split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (words.size < 2) return false
        
        // Check against blacklist
        if (nameBlacklist.any { lowerName.contains(it) }) {
            return false
        }
        
        // Each word should be reasonable length (2-20 characters)
        if (words.any { it.length < 2 || it.length > 20 }) {
            return false
        }
        
        // Should not contain numbers
        if (name.any { it.isDigit() }) {
            return false
        }
        
        // Should not be all uppercase (likely a label/heading)
        if (name == name.uppercase() && name.length > 10) {
            return false
        }
        
        return true
    }
    
    private fun createNameField(name: String, confidence: Int): ExtractedField {
        return ExtractedField(
            fieldName = "firstName",
            fieldLabel = "Full Name",
            value = name,
            confidence = confidence
        )
    }
    
    private fun extractDateOfBirth(text: String): ExtractedField? {
        // Common DOB patterns in Aadhaar
        val patterns = listOf(
            // DD/MM/YYYY or DD-MM-YYYY
            Regex("""(?:dob|birth|जन्म)[\s:]*(\d{2}[/-]\d{2}[/-]\d{4})""", RegexOption.IGNORE_CASE),
            // Year only (Year of Birth: YYYY)
            Regex("""year\s+of\s+birth[\s:]*(\d{4})""", RegexOption.IGNORE_CASE),
            // Just DD/MM/YYYY pattern near DOB keywords
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
    
    private fun extractGender(text: String): ExtractedField? {
        val lowerText = text.lowercase()
        
        return when {
            lowerText.contains("male") && !lowerText.contains("female") -> {
                ExtractedField(
                    fieldName = "gender",
                    fieldLabel = "Gender",
                    value = "Male",
                    confidence = 90
                )
            }
            lowerText.contains("female") -> {
                ExtractedField(
                    fieldName = "gender",
                    fieldLabel = "Gender",
                    value = "Female",
                    confidence = 90
                )
            }
            else -> null
        }
    }
}
