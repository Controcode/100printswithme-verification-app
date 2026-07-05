package com.printswithme.badgeverify.camera

import android.graphics.Rect
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX [ImageAnalysis.Analyzer] that uses ML Kit to detect QR codes.
 * Premium experience: only accepts barcodes centered in the viewfinder and
 * requires 3 consecutive frames of the same value to trigger success.
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    @Volatile private var isAnalyzing = false

    // Debounce state: require 3 consecutive frames of the same QR value
    private var lastValue: String? = null
    private var consecutiveCount = 0
    private val requiredConsecutiveFrames = 3

    fun close() {
        scanner.close()
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // Skip if already analyzing
        if (isAnalyzing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        isAnalyzing = true
        
        // InputImage from mediaImage handles rotation correctly based on rotationDegrees.
        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)
        
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                // Center-targeting: Filter for QR codes whose bounding box intersects the central target area
                val qr = barcodes.firstOrNull { barcode ->
                    barcode.format == Barcode.FORMAT_QR_CODE && 
                    !barcode.rawValue.isNullOrBlank() &&
                    isBarcodeInCenter(barcode, imageProxy)
                }

                if (qr != null) {
                    val value = qr.rawValue!!
                    if (value == lastValue) {
                        consecutiveCount++
                    } else {
                        lastValue = value
                        consecutiveCount = 1
                    }

                    // Stability check: Trigger success only after 3 consecutive identical frames
                    if (consecutiveCount >= requiredConsecutiveFrames) {
                        onBarcodeDetected(value)
                        // Reset debounce state after success
                        consecutiveCount = 0
                        lastValue = null
                    }
                } else {
                    // Reset debounce if frame doesn't contain a valid QR in the center
                    lastValue = null
                    consecutiveCount = 0
                }
            }
            .addOnCompleteListener {
                isAnalyzing = false
                imageProxy.close()
            }
    }

    /**
     * Checks if the barcode's bounding box intersects with the central target area (middle 50% of the frame).
     */
    private fun isBarcodeInCenter(barcode: Barcode, imageProxy: ImageProxy): Boolean {
        val boundingBox = barcode.boundingBox ?: return false
        
        // Use the buffer dimensions. ML Kit bounding box coordinates match these.
        val width = imageProxy.width
        val height = imageProxy.height
        
        val targetRect = Rect(
            (width * 0.25).toInt(),
            (height * 0.25).toInt(),
            (width * 0.75).toInt(),
            (height * 0.75).toInt()
        )
        
        return Rect.intersects(targetRect, boundingBox)
    }
}
