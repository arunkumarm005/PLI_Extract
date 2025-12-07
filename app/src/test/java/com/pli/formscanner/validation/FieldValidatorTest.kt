package com.pli.formscanner.validation

import org.junit.Assert.*
import org.junit.Test

class FieldValidatorTest {

    @Test
    fun `validateAadhaar with valid 12 digit number returns valid`() {
        val result = FieldValidator.validateAadhaar("123456789012")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateAadhaar with spaces returns valid`() {
        val result = FieldValidator.validateAadhaar("1234 5678 9012")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateAadhaar with hyphens returns valid`() {
        val result = FieldValidator.validateAadhaar("1234-5678-9012")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateAadhaar with less than 12 digits returns invalid`() {
        val result = FieldValidator.validateAadhaar("12345678901")
        assertFalse(result.isValid)
        assertEquals("Aadhaar must be 12 digits", result.errorMessage)
    }

    @Test
    fun `validateAadhaar with more than 12 digits returns invalid`() {
        val result = FieldValidator.validateAadhaar("1234567890123")
        assertFalse(result.isValid)
    }

    @Test
    fun `validateAadhaar with letters returns invalid`() {
        val result = FieldValidator.validateAadhaar("12345678901A")
        assertFalse(result.isValid)
        assertEquals("Aadhaar must contain only numbers", result.errorMessage)
    }

    @Test
    fun `validateAadhaar with empty string returns invalid`() {
        val result = FieldValidator.validateAadhaar("")
        assertFalse(result.isValid)
        assertEquals("Aadhaar number is required", result.errorMessage)
    }

    @Test
    fun `validatePAN with valid format returns valid`() {
        val result = FieldValidator.validatePAN("ABCDE1234F")
        assertTrue(result.isValid)
    }

    @Test
    fun `validatePAN with lowercase returns valid`() {
        val result = FieldValidator.validatePAN("abcde1234f")
        assertTrue(result.isValid)
    }

    @Test
    fun `validatePAN with invalid format returns invalid`() {
        val result = FieldValidator.validatePAN("ABC1234567")
        assertFalse(result.isValid)
        assertEquals("Invalid PAN format (e.g., ABCDE1234F)", result.errorMessage)
    }

    @Test
    fun `validatePAN with wrong length returns invalid`() {
        val result = FieldValidator.validatePAN("ABCDE123")
        assertFalse(result.isValid)
        assertEquals("PAN must be 10 characters", result.errorMessage)
    }

    @Test
    fun `validateMobile with valid 10 digit number starting with 6 returns valid`() {
        val result = FieldValidator.validateMobile("6234567890")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateMobile with valid number starting with 9 returns valid`() {
        val result = FieldValidator.validateMobile("9876543210")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateMobile with number starting with 5 returns invalid`() {
        val result = FieldValidator.validateMobile("5234567890")
        assertFalse(result.isValid)
        assertEquals("Mobile must start with 6-9", result.errorMessage)
    }

    @Test
    fun `validateMobile with less than 10 digits returns invalid`() {
        val result = FieldValidator.validateMobile("987654321")
        assertFalse(result.isValid)
        assertEquals("Mobile must be 10 digits", result.errorMessage)
    }

    @Test
    fun `validateMobile with letters returns invalid`() {
        val result = FieldValidator.validateMobile("987654321A")
        assertFalse(result.isValid)
        assertEquals("Mobile must contain only numbers", result.errorMessage)
    }

    @Test
    fun `validatePinCode with valid 6 digit code returns valid`() {
        val result = FieldValidator.validatePinCode("682001")
        assertTrue(result.isValid)
    }

    @Test
    fun `validatePinCode with less than 6 digits returns invalid`() {
        val result = FieldValidator.validatePinCode("68200")
        assertFalse(result.isValid)
        assertEquals("PIN must be 6 digits", result.errorMessage)
    }

    @Test
    fun `validateDateOfBirth with valid date returns valid`() {
        val result = FieldValidator.validateDateOfBirth("15/06/1990")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateDateOfBirth with hyphen separator returns valid`() {
        val result = FieldValidator.validateDateOfBirth("15-06-1990")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateDateOfBirth with invalid day returns invalid`() {
        val result = FieldValidator.validateDateOfBirth("32/06/1990")
        assertFalse(result.isValid)
        assertEquals("Day must be between 1 and 31", result.errorMessage)
    }

    @Test
    fun `validateDateOfBirth with invalid month returns invalid`() {
        val result = FieldValidator.validateDateOfBirth("15/13/1990")
        assertFalse(result.isValid)
        assertEquals("Month must be between 1 and 12", result.errorMessage)
    }

    @Test
    fun `validateDateOfBirth with future year returns invalid`() {
        val result = FieldValidator.validateDateOfBirth("15/06/2025")
        assertFalse(result.isValid)
        assertEquals("Year must be between 1900 and 2024", result.errorMessage)
    }

    @Test
    fun `validateDateOfBirth with 31st April returns invalid`() {
        val result = FieldValidator.validateDateOfBirth("31/04/1990")
        assertFalse(result.isValid)
        assertEquals("Invalid day for this month", result.errorMessage)
    }

    @Test
    fun `validateDateOfBirth with 30th February returns invalid`() {
        val result = FieldValidator.validateDateOfBirth("30/02/1990")
        assertFalse(result.isValid)
        assertEquals("Invalid day for February", result.errorMessage)
    }

    @Test
    fun `validateDateOfBirth with 29th Feb in non-leap year returns invalid`() {
        val result = FieldValidator.validateDateOfBirth("29/02/2021")
        assertFalse(result.isValid)
        assertEquals("Not a leap year", result.errorMessage)
    }

    @Test
    fun `validateDateOfBirth with 29th Feb in leap year returns valid`() {
        val result = FieldValidator.validateDateOfBirth("29/02/2020")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateEmail with valid email returns valid`() {
        val result = FieldValidator.validateEmail("test@example.com")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateEmail with invalid format returns invalid`() {
        val result = FieldValidator.validateEmail("testexample.com")
        assertFalse(result.isValid)
        assertEquals("Invalid email format", result.errorMessage)
    }

    @Test
    fun `validateEmail with missing domain returns invalid`() {
        val result = FieldValidator.validateEmail("test@")
        assertFalse(result.isValid)
    }

    @Test
    fun `validateName with valid name returns valid`() {
        val result = FieldValidator.validateName("John Doe")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateName with single word returns valid`() {
        val result = FieldValidator.validateName("John")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateName with hyphen returns valid`() {
        val result = FieldValidator.validateName("Mary-Jane")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateName with apostrophe returns valid`() {
        val result = FieldValidator.validateName("O'Brien")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateName with single character returns invalid`() {
        val result = FieldValidator.validateName("J")
        assertFalse(result.isValid)
        assertEquals("Name too short", result.errorMessage)
    }

    @Test
    fun `validateName with numbers returns invalid`() {
        val result = FieldValidator.validateName("John123")
        assertFalse(result.isValid)
        assertEquals("Name contains invalid characters", result.errorMessage)
    }

    @Test
    fun `formatFieldValue formats Aadhaar with spaces`() {
        val formatted = FieldValidator.formatFieldValue("adharId", "123456789012")
        assertEquals("1234 5678 9012", formatted)
    }

    @Test
    fun `formatFieldValue formats PAN to uppercase`() {
        val formatted = FieldValidator.formatFieldValue("panNumber", "abcde1234f")
        assertEquals("ABCDE1234F", formatted)
    }

    @Test
    fun `formatFieldValue removes spaces from mobile`() {
        val formatted = FieldValidator.formatFieldValue("mobileNumber", "98765 43210")
        assertEquals("9876543210", formatted)
    }

    @Test
    fun `formatFieldValue formats email to lowercase`() {
        val formatted = FieldValidator.formatFieldValue("emailAddress", "TEST@EXAMPLE.COM")
        assertEquals("test@example.com", formatted)
    }

    @Test
    fun `formatFieldValue capitalizes name properly`() {
        val formatted = FieldValidator.formatFieldValue("firstName", "john DOE")
        assertEquals("John Doe", formatted)
    }
}
