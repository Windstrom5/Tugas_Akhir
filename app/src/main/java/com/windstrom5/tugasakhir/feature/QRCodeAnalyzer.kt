package com.windstrom5.tugasakhir.feature

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.MultiFormatReader
import com.google.zxing.Reader
import com.google.zxing.Result


class QRCodeAnalyzer(
    private val scanningSquare: View,
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader: Reader = MultiFormatReader()

    override fun analyze(imageProxy: ImageProxy) {
        val buffer = imageProxy.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        // Calculate the position of the scanningSquare on the screen
        val squareX = scanningSquare.x.toInt()
        val squareY = scanningSquare.y.toInt()
        val squareWidth = scanningSquare.width
        val squareHeight = scanningSquare.height

        Log.d("QRCodeAnalyzer", "Scanning square coordinates: ($squareX, $squareY), width: $squareWidth, height: $squareHeight")

        // Adjust PlanarYUVLuminanceSource creation based on the scanningSquare's position
        val source = PlanarYUVLuminanceSource(
            data,
            imageProxy.width,
            imageProxy.height,
            squareX,
            squareY,
            squareWidth,
            squareHeight,
            false
        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result: Result = reader.decode(binaryBitmap)
            val scannedText = result.text
            Log.d("QRCodeAnalyzer", "Scanned QR Code: $scannedText")

            // Post the Toast message to the main thread
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    scanningSquare.context,
                    "Scanned QR Code: $scannedText",
                    Toast.LENGTH_SHORT
                ).show()
            }

            onQrCodeScanned(scannedText)
        } catch (e: Exception) {
            Log.e("QRCodeAnalyzer", "Error decoding QR code", e)

            // Post the Toast message to the main thread
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    scanningSquare.context,
                    "Error decoding QR code: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        imageProxy.close()
    }
}
