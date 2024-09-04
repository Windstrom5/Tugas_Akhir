package com.windstrom5.tugasakhir.activity

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley

import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.databinding.ActivitySettingBinding
import com.windstrom5.tugasakhir.fragment.AddDinasFragment
import com.windstrom5.tugasakhir.fragment.HistoryDinasFragment
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import de.hdodenhof.circleimageview.CircleImageView
import java.util.concurrent.Executor
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.core.app.ActivityCompat
import java.util.concurrent.Executors
import android.view.Surface
import android.view.TextureView
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private var bundle:Bundle ?= null
    private var perusahaan: Perusahaan? = null
    private var admin: Admin? = null
    private var pekerja: Pekerja? = null
    private lateinit var addFingerprint:TextView
    private lateinit var addFace:TextView
    private lateinit var profile:CircleImageView
    private lateinit var faceDetector: FaceDetector
    private lateinit var popupWindow: PopupWindow
    private lateinit var textureView: TextureView
    private lateinit var overlayView: ImageView
    private lateinit var cameraExecutor: Executor
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 200
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        profile = binding.profileCircleImageView
        addFace = binding.setfaceDetection
        cameraExecutor = Executors.newSingleThreadExecutor()
        addFingerprint = binding.setFingerprint
        faceDetector = FaceDetection.getClient()
        getBundle()
        if (!isFingerprintScannerAvailable()) {
            addFingerprint.visibility = View.GONE
        }
        addFace.setOnClickListener{
            showFaceCapturePopup()
        }
        addFingerprint.setOnClickListener{
            showBiometricPrompt()
        }
    }
    private fun showFaceCapturePopup() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.activity_custom_camera, null)

        textureView = popupView.findViewById(R.id.texture_view)
        overlayView = popupView.findViewById(R.id.overlay_view)

        popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)

        setupCamera()
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider { request ->
                        val surfaceTexture = textureView.surfaceTexture ?: return@setSurfaceProvider
                        val surface = Surface(surfaceTexture)
                        request.provideSurface(surface, cameraExecutor) { result ->
                            surface.release()   
                        }
                    }
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer())
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalysis)

        }, ContextCompat.getMainExecutor(this))
    }
    private inner class FaceAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            val bitmap = image.toBitmap()
            val inputImage = InputImage.fromBitmap(bitmap, image.imageInfo.rotationDegrees)

            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        // Face detected, draw overlay and capture image
                        drawFaceOverlay(faces, bitmap)
                        captureFaceRecognitionImage(bitmap)
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }
    private fun drawFaceOverlay(faces: List<com.google.mlkit.vision.face.Face>, bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f

        faces.forEach { face ->
            val bounds = face.boundingBox
            canvas.drawRect(bounds, paint)
        }

        runOnUiThread {
            overlayView.setImageBitmap(bitmap)
        }
    }
    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer: ByteBuffer = planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    private fun captureFaceRecognitionImage(bitmap: Bitmap) {
        // Implement image capture logic, e.g., save bitmap from TextureView
        // Ensure the image is clear and centered on the detected face
        runOnUiThread {
            Toast.makeText(this, "Face captured successfully!", Toast.LENGTH_SHORT).show()
            // Save the bitmap to the database or perform other actions
        }
    }
    private fun isFingerprintScannerAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }
    private fun getBundle() {
        bundle = intent?.getBundleExtra("data") ?: return

        perusahaan = bundle!!.getParcelable("perusahaan") ?: return
        val role = bundle!!.getString("role") ?: return

        val url = if (role == "Admin") {
            admin = bundle!!.getParcelable("user") ?: return
            "http://192.168.1.6/Admin/getDecryptedProfile/${admin!!.id}" // Replace with your actual URL
        } else {
            pekerja = bundle!!.getParcelable("user") ?: return
            "http://192.168.1.6/Pekerja/getDecryptedProfile/${pekerja!!.id}" // Replace with your actual URL
        }

        val imageRequest = ImageRequest(
            url,
            { response ->
                // Set the Bitmap to an ImageView or handle it as needed
                profile.setImageBitmap(response)
            },
            0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "Failed to fetch profile image", Toast.LENGTH_SHORT).show()
            }
        )

        // Add the request to the RequestQueue
        Volley.newRequestQueue(this).add(imageRequest)
    }

    private fun showBiometricPrompt() {
        val biometricPrompt = BiometricPrompt(this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Handle error
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val cryptoObject = result.cryptoObject
                    val fingerprintData = cryptoObject?.cipher?.doFinal() // Get fingerprint data

                    admin?.id?.let { updateBiometricData(it,fingerprintData) }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Handle failure
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Scanner")
            .setSubtitle("Place Your Finger In Your Device Scanner")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
    fun updateBiometricData(adminId: Int, biometricData: ByteArray?) {

        val url = "http://192.168.1.6/api/admin/$adminId/updateBiometricData"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .apply {
                biometricData?.let {
                    addFormDataPart("biometric_data", Base64.encodeToString(it, Base64.DEFAULT))
                }

            }
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                // Handle failure
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle success
                } else {
                    // Handle failure
                }
            }
        })
    }
}