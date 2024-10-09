package com.windstrom5.tugasakhir.fragment

import android.content.ContentValues
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.SharedPreferencesManager
import com.windstrom5.tugasakhir.model.Perusahaan
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

class ShowQRCodeFragment : Fragment() {
    private lateinit var imageViewQRCode: ImageView
    private lateinit var imageViewLogoWatermark: ImageView
    private lateinit var downloadButton: Button
    private var perusahaan: Perusahaan? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_show_q_r_code, container, false)
        imageViewQRCode = view.findViewById(R.id.imageViewQRCode)
        imageViewLogoWatermark = view.findViewById(R.id.imageViewLogoWatermark)
        downloadButton = view.findViewById(R.id.downloadQr)
        getBundle()
        val qrCodeBitmap = perusahaan?.secret_key?.let { generateQRCode(it) }
        imageViewQRCode.setImageBitmap(qrCodeBitmap)
        downloadButton.setOnClickListener {
            saveBitmapToGallery((imageViewQRCode.drawable as BitmapDrawable).bitmap, "QRCode")
        }

        return view
    }

    private fun generateQRCodeWithLogo(perusahaan: Perusahaan) {
        try {
//            val secretKeyMD5 = md5(perusahaan.secret_key)
            val qrCodeBitmap = generateQRCode(perusahaan.secret_key, 600, 600)

            // Display the initial QR code without the logo
            imageViewQRCode.setImageBitmap(qrCodeBitmap)

            // Check if the logo is "null" and handle accordingly
            val logoUrl = if (perusahaan.logo == "null") {
                null // Will load from drawable
            } else {
                "https://selected-jaguar-presently.ngrok-free.app/api/Perusahaan/decryptLogo/${perusahaan.id}"
            }

            if (logoUrl == null) {
                // Load the default logo from drawable
                Glide.with(this)
                    .asBitmap()
                    .load(R.drawable.logo) // Load the logo from drawable
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            // Add the default logo as watermark
                            addLogoAsWatermark(qrCodeBitmap, resource, 100)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // Handle when the image loading is cleared
                        }
                    })
            } else {
                // Load the logo dynamically using Volley
                val imageRequest = ImageRequest(
                    logoUrl,
                    { response ->
                        // Add the downloaded logo as watermark to the QR code
                        addLogoAsWatermark(qrCodeBitmap, response, 100)
                    },
                    0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                    { error ->
                        error.printStackTrace()
                        Toast.makeText(requireContext(), "Failed to fetch profile image", Toast.LENGTH_SHORT).show()
                    }
                )
                // Add the request to the queue
                val requestQueue = Volley.newRequestQueue(requireContext())
                requestQueue.add(imageRequest)
            }

        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }


    private fun saveBitmapToGallery(bitmap: Bitmap, fileName: String) {
        val resolver = requireContext().contentResolver

        // Create a new image file in the Pictures directory
        val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val newImage = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val imageUri = resolver.insert(imageCollection, newImage)

        // Write the bitmap to the output stream
        try {
            resolver.openOutputStream(imageUri!!)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                Toast.makeText(requireContext(), "QR code saved to gallery", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to save QR code", Toast.LENGTH_SHORT).show()
        }
    }
    private fun generateQRCode(data: String): Bitmap {
        val size = 512 // size of the QR code
        val bits = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        Log.d("QRCode",data)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }
    private fun downloadQrCode() {
        val qrBitmap = (imageViewQRCode.drawable as BitmapDrawable).bitmap

        val fileName = "QR_Code_${System.currentTimeMillis()}.png"
        val filePath = MediaStore.Images.Media.insertImage(
            requireContext().contentResolver, qrBitmap, fileName, "QR Code"
        )

        if (filePath != null) {
            Toast.makeText(requireContext(), "QR code saved to gallery", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Failed to save QR code", Toast.LENGTH_SHORT).show()
        }
    }
    private fun generateQRCode(content: String, width: Int, height: Int): Bitmap {
        val hints: MutableMap<EncodeHintType, Any> = HashMap()
        hints[EncodeHintType.MARGIN] = 0
        val bitMatrix: BitMatrix =
            MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix[x, y]) -0x1000000 else -0x1
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun addLogoAsWatermark(qrCodeBitmap: Bitmap, logoBitmap: Bitmap, logoSize: Int) {
        val resizedLogo = Bitmap.createScaledBitmap(
            logoBitmap,
            logoSize,
            logoSize,
            false
        )

        // Set the logo watermark to the center of the QR code
        val centerX = (qrCodeBitmap.width - resizedLogo.width) / 2
        val centerY = (qrCodeBitmap.height - resizedLogo.height) / 2

        // Create a new bitmap with the QR code and the logo at the center
        val finalBitmap = Bitmap.createBitmap(
            qrCodeBitmap.width,
            qrCodeBitmap.height,
            qrCodeBitmap.config // Access config through Bitmap.Config
        )

        val canvas = android.graphics.Canvas(finalBitmap)
        canvas.drawBitmap(qrCodeBitmap, 0f, 0f, null)
        canvas.drawBitmap(resizedLogo, centerX.toFloat(), centerY.toFloat(), null)

        imageViewQRCode.setImageBitmap(finalBitmap)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun getBundle() {
        val arguments = arguments
        if (arguments != null) {
            perusahaan = arguments.getParcelable("perusahaan")
//            perusahaan?.let { generateQRCode(it.secret_key) }
        } else {
            Log.d("Error", "Bundle Not Found")
        }
    }
}
