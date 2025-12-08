package com.pli.formscanner

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pli.formscanner.databinding.ActivityScannerBinding
import com.pli.formscanner.models.ExtractedField
import com.pli.formscanner.utils.ImagePreprocessor
import com.pli.formscanner.utils.TextExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val textExtractor = TextExtractor(this)
    private val extractedFields = mutableListOf<ExtractedField>()
    private lateinit var fieldAdapter: FieldAdapter
    
    // State management
    private var isProcessing = false
    private var isContinuousMode = false
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var lastProcessTime = 0L
    private val PROCESS_THROTTLE_MS = 500L // Throttle to 2 fps
    private var continuousScanJob: Job? = null
    
    // Loading state
    private var isLoading = false
    
    // OCR text visibility
    private var isOcrTextExpanded = true
    
    // Multi-scan support
    private var returnToAggregator = false
    private var accumulatedOcrText = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Check if we're returning to aggregator
        returnToAggregator = intent.getBooleanExtra("returnToAggregator", false)

        setupRecyclerView()
        setupUI()
        startCamera()
    }

    private fun setupRecyclerView() {
        fieldAdapter = FieldAdapter(extractedFields)
        binding.rvExtractedFields.apply {
            layoutManager = LinearLayoutManager(this@ScannerActivity)
            adapter = fieldAdapter
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        // OCR Text toggle
        binding.btnToggleOcrText.setOnClickListener {
            isOcrTextExpanded = !isOcrTextExpanded
            binding.ocrTextScrollView.visibility = if (isOcrTextExpanded) View.VISIBLE else View.GONE
            binding.btnToggleOcrText.rotation = if (isOcrTextExpanded) 180f else 0f
        }
        
        binding.btnSaveForm.setOnClickListener { 
            if (extractedFields.isEmpty()) {
                Toast.makeText(this, "No fields detected yet", Toast.LENGTH_SHORT).show()
            } else {
                saveFormData()
            }
        }
        
        // Update button text for multi-scan mode
        if (returnToAggregator) {
            binding.btnSaveForm.text = "Add to Multi-Scan"
        }
        
        // Manual mapping button
        binding.btnManualMapping.setOnClickListener {
            openManualMapping()
        }

        // Manual capture button
        binding.btnCapture.setOnClickListener {
            if (!isProcessing) {
                captureAndProcess()
            }
        }

        // Toggle continuous scanning
        binding.btnToggleScan.setOnClickListener {
            toggleContinuousMode()
        }

        // Flash toggle
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        updateScanModeUI()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                // Image capture for manual mode
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                // Image analyzer for continuous mode (throttled)
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (isContinuousMode) {
                                processImageProxyThrottled(imageProxy)
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
                showError("Failed to start camera: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Throttled image processing for continuous mode
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxyThrottled(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        if (isProcessing || (currentTime - lastProcessTime) < PROCESS_THROTTLE_MS) {
            imageProxy.close()
            return
        }

        lastProcessTime = currentTime
        processImageProxy(imageProxy)
    }

    /**
     * Process image with preprocessing for better OCR
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            isProcessing = true
            showLoading(true)

            lifecycleScope.launch {
                try {
                    // Preprocess image for better OCR
                    val inputImage = ImagePreprocessor.preprocessImage(imageProxy)
                    
                    if (inputImage != null) {
                        processWithMLKit(inputImage)
                    } else {
                        // Fallback to original image
                        val originalImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        processWithMLKit(originalImage)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Image preprocessing failed", e)
                    showError("Processing failed: ${e.message}")
                    isProcessing = false
                    showLoading(false)
                } finally {
                    imageProxy.close()
                }
            }
        } else {
            imageProxy.close()
        }
    }

    /**
     * Process image with ML Kit
     */
    private fun processWithMLKit(image: InputImage) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                lifecycleScope.launch {
                    try {
                        // Display OCR text
                        displayOcrText(visionText.text)
                        
                        // Extract fields
                        val newFields = textExtractor.extractFields(visionText.text)
                        updateExtractedFields(newFields)
                    } catch (e: Exception) {
                        Log.e(TAG, "Field extraction failed", e)
                        showError("Extraction failed: ${e.message}")
                    } finally {
                        isProcessing = false
                        showLoading(false)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed", e)
                showError("Text recognition failed: ${e.message}")
                isProcessing = false
                showLoading(false)
            }
    }

    /**
     * Capture single image and process (manual mode)
     */
    private fun captureAndProcess() {
        showLoading(true)
        
        imageCapture?.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    processImageProxy(imageProxy)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exception)
                    showError("Capture failed: ${exception.message}")
                    showLoading(false)
                }
            }
        )
    }

    /**
     * Toggle between manual and continuous scanning
     */
    private fun toggleContinuousMode() {
        isContinuousMode = !isContinuousMode
        updateScanModeUI()
        
        val message = if (isContinuousMode) {
            "Continuous scanning ON"
        } else {
            "Manual capture mode"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Toggle camera flash/torch
     */
    private fun toggleFlash() {
        camera?.let { cam ->
            val hasFlash = cam.cameraInfo.hasFlashUnit()
            if (hasFlash) {
                val currentState = cam.cameraInfo.torchState.value == TorchState.ON
                cam.cameraControl.enableTorch(!currentState)
                
                binding.btnFlash.setImageResource(
                    if (!currentState) R.drawable.ic_flash else R.drawable.ic_flash_off
                )
                
                Toast.makeText(
                    this,
                    if (!currentState) "Flash ON" else "Flash OFF",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Flash not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateScanModeUI() {
        if (isContinuousMode) {
            binding.btnToggleScan.text = "Stop Auto Scan"
            binding.btnCapture.visibility = View.GONE
        } else {
            binding.btnToggleScan.text = "Auto Scan"
            binding.btnCapture.visibility = View.VISIBLE
        }
    }

    private fun updateExtractedFields(newFields: List<ExtractedField>) {
        runOnUiThread {
            var updatedCount = 0
            var newCount = 0
            
            newFields.forEach { newField ->
                val existingIndex = extractedFields.indexOfFirst { it.fieldName == newField.fieldName }
                if (existingIndex >= 0) {
                    if (newField.confidence > extractedFields[existingIndex].confidence) {
                        extractedFields[existingIndex] = newField
                        updatedCount++
                    }
                } else {
                    extractedFields.add(newField)
                    newCount++
                }
            }
            
            if (updatedCount > 0 || newCount > 0) {
                fieldAdapter.notifyDataSetChanged()
                
                val statusMsg = buildString {
                    append("Found ${extractedFields.size} fields")
                    if (newCount > 0) append(" (+$newCount new)")
                    if (updatedCount > 0) append(" ($updatedCount improved)")
                }
                binding.tvStatus.text = statusMsg
            }
        }
    }

    private fun displayOcrText(text: String) {
        runOnUiThread {
            if (text.isNotBlank()) {
                // Accumulate OCR text for multi-scan
                if (accumulatedOcrText.isNotEmpty()) {
                    accumulatedOcrText.append("\n\n--- Next Scan ---\n\n")
                }
                accumulatedOcrText.append(text)
                
                binding.ocrTextCard.visibility = View.VISIBLE
                binding.tvOcrText.text = accumulatedOcrText.toString()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        runOnUiThread {
            isLoading = show
            binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
            
            if (show) {
                binding.tvStatus.text = "Processing..."
            }
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            Log.e(TAG, message)
        }
    }

    private fun saveFormData() {
        if (returnToAggregator) {
            // Return to aggregator with scan data
            val resultIntent = Intent()
            resultIntent.putExtra("ocrText", accumulatedOcrText.toString())
            resultIntent.putParcelableArrayListExtra("extractedFields", ArrayList(extractedFields))
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            // Go directly to form data
            val intent = Intent(this, FormDataActivity::class.java)
            intent.putParcelableArrayListExtra("extractedFields", ArrayList(extractedFields))
            startActivity(intent)
        }
    }
    
    /**
     * Open manual mapping activity as fallback
     */
    private fun openManualMapping() {
        val ocrText = accumulatedOcrText.toString()
        
        if (ocrText.isBlank()) {
            Toast.makeText(this, "No OCR text available. Please scan a document first.", Toast.LENGTH_LONG).show()
            return
        }
        
        val intent = Intent(this, ManualMappingActivity::class.java)
        intent.putExtra("ocrText", ocrText)
        intent.putExtra("returnToAggregator", returnToAggregator)
        if (returnToAggregator) {
            startActivityForResult(intent, REQUEST_MANUAL_MAPPING)
        } else {
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        continuousScanJob?.cancel()
        cameraExecutor.shutdown()
        textRecognizer.close()
    }

    override fun onPause() {
        super.onPause()
        // Stop continuous scanning when app goes to background
        isContinuousMode = false
        updateScanModeUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_MANUAL_MAPPING && resultCode == RESULT_OK && data != null) {
            // Manual mapping completed, return to aggregator
            setResult(RESULT_OK, data)
            finish()
        }
    }

    companion object {
        private const val TAG = "ScannerActivity"
        private const val REQUEST_MANUAL_MAPPING = 201
    }
}
