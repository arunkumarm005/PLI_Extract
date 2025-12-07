package com.pli.formscanner.validation

/**
 * Centralized validation logic for all form fields.
 * All validation rules are defined here to ensure consistency across the app.
 */
object FieldValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Validates Aadhaar number (12 digits)
     */
    fun validateAadhaar(value: String): ValidationResult {
        val cleaned = value.replace(Regex("[\\s-]"), "")
        
        return when {
            cleaned.isEmpty() -> ValidationResult(false, "Aadhaar number is required")
            cleaned.length != 12 -> ValidationResult(false, "Aadhaar must be 12 digits")
            !cleaned.all { it.isDigit() } -> ValidationResult(false, "Aadhaar must contain only numbers")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates PAN number (ABCDE1234F format)
     */
    fun validatePAN(value: String): ValidationResult {
        val cleaned = value.trim().uppercase()
        val panRegex = Regex("^[A-Z]{5}\\d{4}[A-Z]$")
        
        return when {
            cleaned.isEmpty() -> ValidationResult(false, "PAN number is required")
            cleaned.length != 10 -> ValidationResult(false, "PAN must be 10 characters")
            !panRegex.matches(cleaned) -> ValidationResult(false, "Invalid PAN format (e.g., ABCDE1234F)")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates Indian mobile number (10 digits starting with 6-9)
     */
    fun validateMobile(value: String): ValidationResult {
        val cleaned = value.replace(Regex("[\\s-]"), "")
        
        return when {
            cleaned.isEmpty() -> ValidationResult(false, "Mobile number is required")
            cleaned.length != 10 -> ValidationResult(false, "Mobile must be 10 digits")
            !cleaned.all { it.isDigit() } -> ValidationResult(false, "Mobile must contain only numbers")
            cleaned[0] !in '6'..'9' -> ValidationResult(false, "Mobile must start with 6-9")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates Indian PIN code (6 digits)
     */
    fun validatePinCode(value: String): ValidationResult {
        val cleaned = value.trim()
        
        return when {
            cleaned.isEmpty() -> ValidationResult(false, "PIN code is required")
            cleaned.length != 6 -> ValidationResult(false, "PIN must be 6 digits")
            !cleaned.all { it.isDigit() } -> ValidationResult(false, "PIN must contain only numbers")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates date of birth (DD/MM/YYYY or DD-MM-YYYY)
     */
    fun validateDateOfBirth(value: String): ValidationResult {
        val cleaned = value.trim()
        
        if (cleaned.isEmpty()) {
            return ValidationResult(false, "Date of birth is required")
        }

        val parts = cleaned.split(Regex("[/-]"))
        
        if (parts.size != 3) {
            return ValidationResult(false, "Invalid date format (use DD/MM/YYYY)")
        }

        val day = parts[0].toIntOrNull()
        val month = parts[1].toIntOrNull()
        val year = parts[2].toIntOrNull()

        return when {
            day == null || month == null || year == null -> 
                ValidationResult(false, "Date must contain valid numbers")
            day !in 1..31 -> 
                ValidationResult(false, "Day must be between 1 and 31")
            month !in 1..12 -> 
                ValidationResult(false, "Month must be between 1 and 12")
            year !in 1900..2024 -> 
                ValidationResult(false, "Year must be between 1900 and 2024")
            // Additional date validation
            month in listOf(4, 6, 9, 11) && day > 30 ->
                ValidationResult(false, "Invalid day for this month")
            month == 2 && day > 29 ->
                ValidationResult(false, "Invalid day for February")
            month == 2 && day == 29 && !isLeapYear(year) ->
                ValidationResult(false, "Not a leap year")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates email address
     */
    fun validateEmail(value: String): ValidationResult {
        val cleaned = value.trim().lowercase()
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        
        return when {
            cleaned.isEmpty() -> ValidationResult(false, "Email address is required")
            !emailRegex.matches(cleaned) -> ValidationResult(false, "Invalid email format")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates name (supports multiple formats)
     */
    fun validateName(value: String): ValidationResult {
        val cleaned = value.trim()
        
        return when {
            cleaned.isEmpty() -> ValidationResult(false, "Name is required")
            cleaned.length < 2 -> ValidationResult(false, "Name too short")
            cleaned.length > 100 -> ValidationResult(false, "Name too long")
            // Allow letters, spaces, dots, hyphens, and apostrophes
            !cleaned.matches(Regex("^[a-zA-Z .'-]+$")) -> 
                ValidationResult(false, "Name contains invalid characters")
            else -> ValidationResult(true)
        }
    }

    /**
     * Generic validation dispatcher
     */
    fun validateField(fieldName: String, value: String): ValidationResult {
        return when (fieldName) {
            "adharId" -> validateAadhaar(value)
            "panNumber" -> validatePAN(value)
            "mobileNumber" -> validateMobile(value)
            "zip" -> validatePinCode(value)
            "birthdate" -> validateDateOfBirth(value)
            "emailAddress" -> validateEmail(value)
            "firstName" -> validateName(value)
            else -> if (value.isBlank()) {
                ValidationResult(false, "This field is required")
            } else {
                ValidationResult(true)
            }
        }
    }

    /**
     * Check if year is a leap year
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * Format field value according to type
     */
    fun formatFieldValue(fieldName: String, value: String): String {
        return when (fieldName) {
            "adharId" -> {
                val cleaned = value.replace(Regex("[\\s-]"), "")
                cleaned.chunked(4).joinToString(" ")
            }
            "panNumber" -> value.uppercase().trim()
            "mobileNumber" -> value.replace(Regex("[\\s-]"), "")
            "emailAddress" -> value.lowercase().trim()
            "firstName" -> value.trim().split(" ")
                .joinToString(" ") { word -> 
                    word.lowercase().replaceFirstChar { it.uppercase() } 
                }
            else -> value.trim()
        }
    }

    /**
     * Get field requirements for UI display
     */
    fun getFieldRequirements(fieldName: String): String {
        return when (fieldName) {
            "adharId" -> "12 digits (spaces/hyphens optional)"
            "panNumber" -> "10 characters (e.g., ABCDE1234F)"
            "mobileNumber" -> "10 digits starting with 6-9"
            "zip" -> "6 digits"
            "birthdate" -> "DD/MM/YYYY format"
            "emailAddress" -> "Valid email format"
            "firstName" -> "At least 2 characters"
            else -> ""
        }
    }
}
