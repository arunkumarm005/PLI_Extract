package com.pli.formscanner.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Utility for preprocessing images before OCR to improve accuracy.
 * Handles rotation, contrast enhancement, and noise reduction.
 */
object ImagePreprocessor {

    /**
     * Preprocess ImageProxy for better OCR results
     */
    suspend fun preprocessImage(imageProxy: ImageProxy): InputImage? = withContext(Dispatchers.Default) {
        try {
            val bitmap = imageProxy.toBitmap() ?: return@withContext null
            val processedBitmap = enhanceForOCR(bitmap)
            
            InputImage.fromBitmap(
                processedBitmap,
                imageProxy.imageInfo.rotationDegrees
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Preprocess Bitmap for better OCR results
     */
    suspend fun preprocessBitmap(bitmap: Bitmap, rotation: Int = 0): Bitmap = withContext(Dispatchers.Default) {
        var processed = bitmap
        
        // Rotate if needed
        if (rotation != 0) {
            processed = rotateBitmap(processed, rotation.toFloat())
        }
        
        // Enhance for OCR
        processed = enhanceForOCR(processed)
        
        processed
    }

    /**
     * Enhance image for OCR by improving contrast and reducing noise
     */
    private fun enhanceForOCR(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to grayscale and enhance contrast
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            
            // Grayscale conversion
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            
            // Contrast enhancement using histogram stretching
            val enhanced = enhanceContrast(gray)
            
            // Apply threshold for better text detection
            val thresholded = if (enhanced > 180) 255 else enhanced
            
            pixels[i] = (0xff shl 24) or (thresholded shl 16) or (thresholded shl 8) or thresholded
        }

        val enhancedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        enhancedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        
        return enhancedBitmap
    }

    /**
     * Enhance contrast using simple linear stretching
     */
    private fun enhanceContrast(value: Int): Int {
        // Simple contrast enhancement
        val factor = 1.5
        val enhanced = ((value - 128) * factor + 128).toInt()
        return enhanced.coerceIn(0, 255)
    }

    /**
     * Rotate bitmap by specified degrees
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        
        val matrix = Matrix()
        matrix.postRotate(degrees)
        
        return Bitmap.createBitmap(
            bitmap, 0, 0, 
            bitmap.width, bitmap.height, 
            matrix, true
        )
    }

    /**
     * Crop bitmap to specified region
     */
    fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        val safeX = x.coerceIn(0, bitmap.width - 1)
        val safeY = y.coerceIn(0, bitmap.height - 1)
        val safeWidth = width.coerceIn(1, bitmap.width - safeX)
        val safeHeight = height.coerceIn(1, bitmap.height - safeY)
        
        return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth, safeHeight)
    }

    /**
     * Scale bitmap to optimal size for OCR
     */
    fun scaleForOCR(bitmap: Bitmap): Bitmap {
        val maxDimension = 1920
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        val scale = if (width > height) {
            maxDimension.toFloat() / width
        } else {
            maxDimension.toFloat() / height
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Apply adaptive threshold for better text detection
     */
    fun applyAdaptiveThreshold(bitmap: Bitmap, blockSize: Int = 15): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val grayPixels = IntArray(width * height)
        
        // Convert to grayscale
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            grayPixels[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }
        
        // Apply adaptive thresholding
        for (y in 0 until height) {
            for (x in 0 until width) {
                val sum = calculateLocalAverage(grayPixels, width, height, x, y, blockSize)
                val pixel = grayPixels[y * width + x]
                
                pixels[y * width + x] = if (pixel > sum) {
                    0xFFFFFFFF.toInt() // White
                } else {
                    0xFF000000.toInt() // Black
                }
            }
        }
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Calculate local average for adaptive thresholding
     */
    private fun calculateLocalAverage(
        pixels: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        blockSize: Int
    ): Int {
        val half = blockSize / 2
        var sum = 0
        var count = 0
        
        for (dy in -half..half) {
            for (dx in -half..half) {
                val px = x + dx
                val py = y + dy
                
                if (px in 0 until width && py in 0 until height) {
                    sum += pixels[py * width + px]
                    count++
                }
            }
        }
        
        return if (count > 0) sum / count else 0
    }

    /**
     * Convert ImageProxy to Bitmap
     */
    private fun ImageProxy.toBitmap(): Bitmap? {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        return try {
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compress bitmap to reduce file size
     */
    fun compressBitmap(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /**
     * Get rotation from EXIF data
     */
    fun getExifRotation(exif: ExifInterface): Int {
        return when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }
}
