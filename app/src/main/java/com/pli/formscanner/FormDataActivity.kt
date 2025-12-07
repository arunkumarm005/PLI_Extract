package com.pli.formscanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.gson.GsonBuilder
import com.pli.formscanner.database.FormRepository
import com.pli.formscanner.databinding.ActivityFormDataBinding
import com.pli.formscanner.models.ExtractedField
import com.pli.formscanner.validation.FieldValidator
import kotlinx.coroutines.launch
import java.io.File

class FormDataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFormDataBinding
    private val extractedFields = mutableListOf<ExtractedField>()
    private val editTextMap = mutableMapOf<String, EditText>()
    private lateinit var repository: FormRepository
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = FormRepository(this)

        intent.getParcelableArrayListExtra<ExtractedField>("extractedFields")?.let {
            extractedFields.addAll(it)
        }

        setupToolbar()
        displayEditableForm()

        binding.btnExport.setOnClickListener { exportToJson() }
        binding.btnShare.setOnClickListener { shareJson() }
        binding.btnSubmit.setOnClickListener { submitForm() }
        binding.btnSaveDraft.setOnClickListener { saveDraft() }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Verify & Edit Form Data"
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun displayEditableForm() {
        binding.formContainer.removeAllViews()

        // Group fields by priority
        val aadhaarField = extractedFields.find { it.fieldName == "adharId" }
        val panField = extractedFields.find { it.fieldName == "panNumber" }
        val mobileField = extractedFields.find { it.fieldName == "mobileNumber" }
        val dobField = extractedFields.find { it.fieldName == "birthdate" }
        val nameField = extractedFields.find { it.fieldName == "firstName" }
        val emailField = extractedFields.find { it.fieldName == "emailAddress" }
        val pinField = extractedFields.find { it.fieldName == "zip" }

        // Add section header
        addSectionHeader("Identity Information")

        // Aadhaar
        aadhaarField?.let { addEditableField(it) }
            ?: addEmptyField("adharId", "Aadhaar Number")

        // PAN
        panField?.let { addEditableField(it) }
            ?: addEmptyField("panNumber", "PAN Number")

        addSectionHeader("Personal Details")

        // Name
        nameField?.let { addEditableField(it) }
            ?: addEmptyField("firstName", "Full Name")

        // Date of Birth
        dobField?.let { addEditableField(it) }
            ?: addEmptyField("birthdate", "Date of Birth")

        addSectionHeader("Contact Information")

        // Mobile
        mobileField?.let { addEditableField(it) }
            ?: addEmptyField("mobileNumber", "Mobile Number")

        // Email
        emailField?.let { addEditableField(it) }
            ?: addEmptyField("emailAddress", "Email Address")

        // PIN Code
        pinField?.let { addEditableField(it) }
            ?: addEmptyField("zip", "PIN Code")
    }

    private fun addSectionHeader(title: String) {
        val headerView = TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 32, 0, 16)
        }
        binding.formContainer.addView(headerView)
    }

    private fun addEditableField(field: ExtractedField) {
        val cardView = createFieldCard()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        // Label with requirements
        val labelText = "${field.fieldLabel} - ${FieldValidator.getFieldRequirements(field.fieldName)}"
        val label = TextView(this).apply {
            text = labelText
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // EditText with value
        val editText = EditText(this).apply {
            setText(field.value)
            hint = FieldValidator.getFieldRequirements(field.fieldName)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            background = null
            setPadding(0, 16, 0, 16)
        }

        // Confidence and validation indicator container
        val bottomContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)
        }

        // Confidence badge
        val confidenceBadge = TextView(this).apply {
            text = "OCR: ${field.confidence}%"
            textSize = 10f
            setPadding(16, 8, 16, 8)
            setBackgroundColor(getConfidenceColor(field.confidence))
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }

        // Validation icon and error message
        val validationIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                marginStart = 16
            }
            visibility = View.GONE
        }

        val errorText = TextView(this).apply {
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            visibility = View.GONE
            setPadding(0, 8, 0, 0)
        }

        bottomContainer.addView(confidenceBadge)
        bottomContainer.addView(validationIcon)

        // Add real-time validation using centralized validator
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val validationResult = FieldValidator.validateField(field.fieldName, s.toString())
                validationIcon.visibility = View.VISIBLE
                
                if (validationResult.isValid) {
                    validationIcon.setImageResource(android.R.drawable.checkbox_on_background)
                    validationIcon.setColorFilter(ContextCompat.getColor(this@FormDataActivity, android.R.color.holo_green_dark))
                    errorText.visibility = View.GONE
                } else {
                    validationIcon.setImageResource(android.R.drawable.ic_delete)
                    validationIcon.setColorFilter(ContextCompat.getColor(this@FormDataActivity, android.R.color.holo_red_dark))
                    errorText.text = validationResult.errorMessage
                    errorText.visibility = View.VISIBLE
                }
            }
        })

        editTextMap[field.fieldName] = editText

        container.addView(label)
        container.addView(editText)
        container.addView(bottomContainer)
        container.addView(errorText)
        cardView.addView(container)
        binding.formContainer.addView(cardView)
    }

    private fun addEmptyField(fieldName: String, label: String) {
        val cardView = createFieldCard()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val labelText = "$label - ${FieldValidator.getFieldRequirements(fieldName)}"
        val labelView = TextView(this).apply {
            text = labelText
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val editText = EditText(this).apply {
            hint = FieldValidator.getFieldRequirements(fieldName)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            background = null
            setPadding(0, 16, 0, 16)
        }

        val notDetectedText = TextView(this).apply {
            text = "⚠ Not detected - Please enter manually"
            textSize = 10f
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
            setPadding(0, 8, 0, 0)
        }

        val errorText = TextView(this).apply {
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            visibility = View.GONE
            setPadding(0, 8, 0, 0)
        }

        // Add validation
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val validationResult = FieldValidator.validateField(fieldName, s.toString())
                if (!validationResult.isValid && s.toString().isNotEmpty()) {
                    errorText.text = validationResult.errorMessage
                    errorText.visibility = View.VISIBLE
                } else {
                    errorText.visibility = View.GONE
                }
            }
        })

        editTextMap[fieldName] = editText

        container.addView(labelView)
        container.addView(editText)
        container.addView(notDetectedText)
        container.addView(errorText)
        cardView.addView(container)
        binding.formContainer.addView(cardView)
    }

    private fun createFieldCard(): MaterialCardView {
        return MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            cardElevation = 4f
            radius = 16f
            setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
        }
    }

    private fun getConfidenceColor(confidence: Int): Int {
        return when {
            confidence >= 85 -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
            confidence >= 70 -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
        }
    }

    private fun getFormDataMap(): Map<String, String> {
        val formData = mutableMapOf<String, String>()
        editTextMap.forEach { (fieldName, editText) ->
            val value = editText.text.toString().trim()
            if (value.isNotBlank()) {
                formData[fieldName] = FieldValidator.formatFieldValue(fieldName, value)
            }
        }
        return formData
    }

    private fun exportToJson() {
        if (isLoading) return
        
        showLoading(true)
        
        val formData = mutableMapOf<String, Any>()

        editTextMap.forEach { (fieldName, editText) ->
            val value = editText.text.toString().trim()
            if (value.isNotBlank()) {
                val validationResult = FieldValidator.validateField(fieldName, value)
                formData[fieldName] = mapOf(
                    "value" to FieldValidator.formatFieldValue(fieldName, value),
                    "valid" to validationResult.isValid,
                    "error" to (validationResult.errorMessage ?: "")
                )
            }
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(formData)

        try {
            val file = File(getExternalFilesDir(null), "pli_form_${System.currentTimeMillis()}.json")
            file.writeText(json)

            showLoading(false)
            Toast.makeText(this, "✓ Exported to:\n${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share exported JSON file
     */
    private fun shareJson() {
        val formData = mutableMapOf<String, Any>()

        editTextMap.forEach { (fieldName, editText) ->
            val value = editText.text.toString().trim()
            if (value.isNotBlank()) {
                val validationResult = FieldValidator.validateField(fieldName, value)
                formData[fieldName] = mapOf(
                    "value" to FieldValidator.formatFieldValue(fieldName, value),
                    "valid" to validationResult.isValid
                )
            }
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(formData)

        try {
            val file = File(getExternalFilesDir(null), "pli_form_${System.currentTimeMillis()}.json")
            file.writeText(json)

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "PLI Form Data")
                putExtra(Intent.EXTRA_TEXT, "Scanned form data from PLI Form Scanner")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share form data"))

        } catch (e: Exception) {
            Toast.makeText(this, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Save form as draft to database
     */
    private fun saveDraft() {
        if (isLoading) return
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val formData = getFormDataMap()
                val id = repository.saveFormData(formData, isCompleted = false)
                
                showLoading(false)
                Toast.makeText(this@FormDataActivity, "✓ Draft saved (ID: $id)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@FormDataActivity, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitForm() {
        val requiredFields = listOf("adharId", "mobileNumber", "firstName")
        val invalidFields = mutableListOf<String>()
        val missingFields = mutableListOf<String>()

        requiredFields.forEach { fieldName ->
            val editText = editTextMap[fieldName]
            val value = editText?.text.toString().trim()

            if (value.isBlank()) {
                missingFields.add(fieldName)
            } else {
                val validationResult = FieldValidator.validateField(fieldName, value)
                if (!validationResult.isValid) {
                    invalidFields.add(fieldName)
                }
            }
        }

        when {
            missingFields.isNotEmpty() -> {
                Toast.makeText(
                    this,
                    "❌ Please fill required fields:\n${missingFields.joinToString(", ")}",
                    Toast.LENGTH_LONG
                ).show()
            }
            invalidFields.isNotEmpty() -> {
                Toast.makeText(
                    this,
                    "❌ Please correct invalid fields:\n${invalidFields.joinToString(", ")}",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                // Save to database
                showLoading(true)
                lifecycleScope.launch {
                    try {
                        val formData = getFormDataMap()
                        val id = repository.saveFormData(formData, isCompleted = true)
                        
                        showLoading(false)
                        
                        AlertDialog.Builder(this@FormDataActivity)
                            .setTitle("Success")
                            .setMessage("Form submitted successfully!\nForm ID: $id")
                            .setPositiveButton("OK") { _, _ -> finish() }
                            .show()
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(this@FormDataActivity, "Submission failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        isLoading = show
        runOnUiThread {
            binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
            binding.btnExport.isEnabled = !show
            binding.btnShare.isEnabled = !show
            binding.btnSubmit.isEnabled = !show
            binding.btnSaveDraft.isEnabled = !show
        }
    }
}
