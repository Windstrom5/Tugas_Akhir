package com.windstrom5.tugasakhir.fragment

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.Lottie
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.bumptech.glide.Glide
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.activity.UserActivity
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.connection.SharedPreferencesManager
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.SecretKeyInfo
import org.json.JSONException
import org.json.JSONObject
import com.windstrom5.tugasakhir.connection.Tracking
import com.windstrom5.tugasakhir.databinding.FragmentScanAbsensiBinding
import com.windstrom5.tugasakhir.feature.EmailSender
import com.windstrom5.tugasakhir.feature.QRCodeAnalyzer
import com.windstrom5.tugasakhir.model.Absen
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.DinasItem
import com.windstrom5.tugasakhir.model.IzinItem
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.ScanQRCode
import io.github.g00fy2.quickie.config.BarcodeFormat
import io.github.g00fy2.quickie.config.ScannerConfig
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import okhttp3.ResponseBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ScanAbsensiFragment : Fragment() {
//    private val scanCustomCode = registerForActivityResult(ScanCustomCode(), ::handleResult)
    private lateinit var requestQueue: RequestQueue // Add this line
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var button : Button
    private lateinit var logo : ImageView
    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private val CAMERA_REQUEST_CODE = 101
    private var perusahaan: Perusahaan? = null
    private lateinit var trackingIntent: Intent
    private var pekerja: Pekerja? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var keterangan: String ?= null
    private var codeScanner: CodeScanner? = null
    private lateinit var binding: FragmentScanAbsensiBinding
    private lateinit var viewKonfetti: KonfettiView
    private lateinit var textView: TextView
    private lateinit var lottie: LottieAnimationView
    private lateinit var scannerView: CodeScannerView
    private val holidaysMap = mutableMapOf<Calendar, String>()
    private val selectedDays = mutableListOf<String>()
    private var adminList: List<Admin>? = null
    private val dayTranslations = mapOf(
        "Senin" to "Monday",
        "Selasa" to "Tuesday",
        "Rabu" to "Wednesday",
        "Kamis" to "Thursday",
        "Jumat" to "Friday",
        "Sabtu" to "Saturday",
        "Minggu" to "Sunday"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan_absensi, container, false)
    }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Start scanning or show holiday animation if applicable
                Log.d("ScanAbsensiFragment", "Permission granted")
                if (isTodayHolidayOrSelectedDay()) {
                    Log.d("ScanAbsensiFragment", "Today is a holiday")
                    showHolidayAnimation()
                } else if(fetchDataIzin()){
                    showizinAnimation()
                }else if(fetchDataDinas()){
                    showdinasAnimation()
                }else {
                    Log.d("ScanAbsensiFragment", "Starting QR code scanner")
                    perusahaan?.let { fetchDataFromApi(it.nama) }
                    startCodeScanner()
                }
            } else {
                // Permission is denied. Handle the denial.
                Log.d("ScanAbsensiFragment", "Permission denied")
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lottie = view.findViewById(R.id.lottie)
        textView = view.findViewById(R.id.textView)
        fetchHolidayData()
        checkLocationPermission()
        scannerView = view.findViewById(R.id.scanner_view)
        textView.setText("Put The QR Code Inside The Box")
        codeScanner = CodeScanner(requireActivity(), scannerView)
        cameraExecutor = Executors.newSingleThreadExecutor()
        logo = view.findViewById(R.id.logoImage)
        viewKonfetti = view.findViewById(R.id.konfettiView)
        requestQueue = Volley.newRequestQueue(requireContext())
        trackingIntent = Intent(requireContext(), Tracking::class.java)
        getBundle()
        val logo2 = perusahaan?.logo
        Log.d("Logo",logo2.toString())
//        if(logo2 == "null"){
//            Glide.with(this)
//                .load(R.drawable.logo)
//                .into(logo)
//        }else{
//            val imageUrl =
//                "https://selected-jaguar-presently.ngrok-free.app/storage/${perusahaan?.logo}" // Replace with your Laravel image URL
//
//            Glide.with(this)
//                .load(imageUrl)
//                .into(logo)
//        }
        if (isTodayHolidayOrSelectedDay()) {
            Log.d("ScanAbsensiFragment", "Today is a holiday - showing holiday animation")
            showHolidayAnimation()
        } else {
            Log.d("ScanAbsensiFragment", "Not a holiday - checking camera permission")
//            if (isInternetAvailable()) {
                checkCameraPermissionAndStartScanner()
//            } else {
//                // Show Toast if no internet connection
//                Toast.makeText(requireContext(), "No internet connection. Please check your connection.", Toast.LENGTH_LONG).show()
//            }

        }
    }
//private suspend fun isInternetAvailable(): Boolean {
//    return withContext(Dispatchers.IO) {
//        try {
//            val url = URL("https://selected-jaguar-presently.ngrok-free.app") // Replace with your actual endpoint
//            val connection = url.openConnection() as HttpURLConnection
//            connection.requestMethod = "HEAD" // Use HEAD to only get headers
//            connection.connectTimeout = 3000 // Timeout in milliseconds
//            connection.connect()
//            val responseCode = connection.responseCode
//            responseCode in 200..399 // Check if the response code indicates success (200-399)
//        } catch (e: Exception) {
//            false // If an exception occurs, consider it as no internet
//        }
//    }
//}
private fun fetchDataFromApi(namaPerusahaan: String) {
    val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
    Log.d("FetchDataError", "Nama: $namaPerusahaan")

    val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService = retrofit.create(ApiService::class.java)

    val call = apiService.getDataPekerja(namaPerusahaan)
    call.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    try {
                        val responseData = JSONObject(responseBody.string())
                        val adminArray = responseData.getJSONArray("admin")
                        // Parse admin and pekerja data
                        adminList = parseAdminList(adminArray)  // Assign parsed admin list
                    } catch (e: JSONException) {
                        Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                    }
                }
            } else {
                // Handle unsuccessful response
                Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            // Handle network failures
            Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
        }
    })
}
    private fun parseAdminList(adminArray: JSONArray): List<Admin> {
        val adminList = mutableListOf<Admin>()
        for (i in 0 until adminArray.length()) {
            val adminObject = adminArray.getJSONObject(i)
            adminList.add(
                Admin(
                    adminObject.getInt("id"),
                    adminObject.getInt("id_perusahaan"),
                    adminObject.getString("email"),
                    adminObject.getString("password"),
                    adminObject.getString("nama"),
                    parseDate(adminObject.getString("tanggal_lahir")),
                    adminObject.getString("profile")
                )
            )
        }
        return adminList
    }
    private fun parseDate(dateString: String): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }
    private fun checkCameraPermissionAndStartScanner() {
        // Check if the camera permission is already granted, if not request it
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            Log.d("ScanAbsensiFragment", "Camera permission already granted")
            startCodeScanner()
        } else {
            Log.d("ScanAbsensiFragment", "Requesting camera permission")
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
        }
    }

    private fun startCodeScanner() {
        Log.d("ScanAbsensiFragment", "Initializing QR code scanner")

        codeScanner?.apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS
            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            isFlashEnabled = false

            decodeCallback = DecodeCallback { result ->
                activity?.runOnUiThread {
                    Log.d("ScanAbsensiFragment", "QR code detected: ${result.text}")
                    getAllSecretKeysFromApi(result.text)
                }
            }

            errorCallback = ErrorCallback { error ->
                activity?.runOnUiThread {
                    Log.e("ScanAbsensiFragment", "Camera error: ${error.message}")
                    Toast.makeText(requireContext(), "Camera error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        scannerView.visibility = View.VISIBLE
        lottie.visibility = View.GONE
        codeScanner?.startPreview()
        Log.d("ScanAbsensiFragment", "QR code scanner preview started")
    }


    private fun showHolidayAnimation() {
        // Stop scanning and show holiday animation
        codeScanner?.stopPreview()
        scannerView.visibility = View.GONE
        lottie.apply {
            repeatCount = LottieDrawable.INFINITE
            setAnimation(R.raw.freeday)
            visibility = View.VISIBLE
            playAnimation()
        }
        Toast.makeText(requireContext(), "Today is a holiday. Scanning is disabled.", Toast.LENGTH_LONG).show()
    }

    private fun showizinAnimation() {
        // Stop scanning and show holiday animation
        codeScanner?.stopPreview()
        scannerView.visibility = View.GONE
        lottie.apply {
            repeatCount = LottieDrawable.INFINITE
            setAnimation(R.raw.izin)
            visibility = View.VISIBLE
            playAnimation()
        }
        val messages = mapOf(
            "Sakit" to "Semoga cepat sembuh dan dapat kembali bekerja dengan sehat!",
            "Cuti" to "Selamat menikmati cuti! Kami menunggu kehadiran Anda kembali.",
            "Izin Khusus" to "Izin khusus telah diterima. Pastikan semua keperluan Anda terpenuhi.",
            "Pendidikan" to "Semoga kegiatan pendidikan Anda berjalan lancar!",
            "Liburan" to "Selamat menikmati liburan! Jangan lupa untuk beristirahat.",
            "Keperluan Pribadi" to "Semoga semua keperluan pribadi Anda terselesaikan dengan baik.",
            "Kegiatan Keluarga" to "Semoga kegiatan keluarga Anda berjalan dengan lancar!",
            "Ibadah" to "Semoga ibadah Anda diterima dan membawa berkah."
        )
        val message = messages[keterangan] ?: "Kategori tidak diketahui."
        textView.text = message
        Toast.makeText(requireContext(), "Today is your off day. Scanning is disabled.", Toast.LENGTH_LONG).show()
    }

    private fun showdinasAnimation() {
        // Stop scanning and show holiday animation
        codeScanner?.stopPreview()
        scannerView.visibility = View.GONE
        lottie.apply {
            repeatCount = LottieDrawable.INFINITE
            setAnimation(R.raw.dinas)
            visibility = View.VISIBLE
            playAnimation()
        }
        textView.text = "Semoga perjalanan dinas Anda ke $keterangan lancar"


        Toast.makeText(requireContext(), "Today is your off day. Scanning is disabled.", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Resume scanning if today is not a holiday
        if (!isTodayHolidayOrSelectedDay()) {
            codeScanner?.startPreview()
        }
    }

    override fun onPause() {
        // Release resources when the fragment is paused
        codeScanner?.releaseResources()
        super.onPause()
    }
    private fun fetchHolidayData() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val url = "https://dayoffapi.vercel.app/api?year=$currentYear"

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                holidaysMap.clear() // Clear previous data

                for (i in 0 until response.length()) {
                    val holidayObject = response.getJSONObject(i)
                    val date = holidayObject.getString("tanggal")
                    val description = holidayObject.getString("keterangan")

                    // Parse date string into Calendar object
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val holidayDate = Calendar.getInstance().apply {
                        time = dateFormat.parse(date)
                    }

                    holidaysMap[holidayDate] = description
                }
            },
            { error ->
                error.printStackTrace()
            }
        )

        Volley.newRequestQueue(requireContext()).add(jsonArrayRequest)
    }
    private fun isTodayHolidayOrSelectedDay(): Boolean {
        val today = Calendar.getInstance()
        val dayName = SimpleDateFormat("EEEE", Locale("id", "ID")).format(today.time)  // Get today's day name in full

        // Check if today is a company-specific holiday (selectedDays)
        val isHoliday = selectedDays.any { it.equals(dayName, ignoreCase = true) }

        // Check if today is a national holiday
        val isNationalHoliday = isTodayNationalHoliday()

        // List of disabled weekdays based on perusahaan holiday data
        val disabledWeekdays = mutableListOf<Int>()

        // Split the perusahaan holiday data into individual days and process them
        perusahaan?.holiday?.split(",\\s*".toRegex())?.forEach { day ->
            when (day.trim().lowercase()) {
                "senin" -> disabledWeekdays.add(Calendar.MONDAY)
                "selasa" -> disabledWeekdays.add(Calendar.TUESDAY)
                "rabu" -> disabledWeekdays.add(Calendar.WEDNESDAY)
                "kamis" -> disabledWeekdays.add(Calendar.THURSDAY)
                "jumat" -> disabledWeekdays.add(Calendar.FRIDAY)
                "sabtu" -> disabledWeekdays.add(Calendar.SATURDAY)
                "minggu" -> disabledWeekdays.add(Calendar.SUNDAY)
                "nasional" -> {
                    // If "Nasional" is mentioned, mark it as a national holiday
                    if (isNationalHoliday) {
                        textView.text = "Today is a national holiday. Enjoy your day off!"
                    }
                }
            }
        }

        // Check if today matches any of the disabled weekdays (company holidays)
        val isCompanyHoliday = disabledWeekdays.contains(today.get(Calendar.DAY_OF_WEEK))

        // Logging the checks for debugging
        Log.d("Holiday", "Today is $dayName")
        Log.d("Holiday", "Selected days: $selectedDays")
        Log.d("HolidayCheck", "Is today a selected company holiday? $isHoliday")
        Log.d("HolidayCheck", "Is today a national holiday? $isNationalHoliday")
        Log.d("HolidayCheck", "Is today a company weekday holiday? $isCompanyHoliday")

        // Display a message if today is a company holiday or a national holiday
        if (isHoliday) {
            textView.text = "Today is a company holiday: $dayName. Enjoy your day off!"
        } else if (isCompanyHoliday) {
            textView.text = "Today is a company holiday: $dayName. Enjoy your day off!"
        }

        // Return true if today is either a company holiday or a national holiday
        return isHoliday || isNationalHoliday || isCompanyHoliday
    }
    private fun isDateToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val givenDate = Calendar.getInstance()
        givenDate.time = date
        return today.get(Calendar.YEAR) == givenDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == givenDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun fetchDataIzin():Boolean {
        var isTodayMatched = false
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val call = perusahaan?.let { pekerja?.let { it1 -> apiService.getDataIzinPekerja(it.nama, it1.nama) } }
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")
                            val statusWithIzinMap = mutableMapOf<String, MutableList<IzinItem>>()
                            var hasTodayData = false

                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val status = jsonObject.getString("status")
                                val tanggal = parseDate(jsonObject.getString("tanggal"))
                                // Check if today's date matches
                                if (isDateToday(tanggal) && status == "Accept") {
                                    keterangan = jsonObject.getString("kategori")
                                    isTodayMatched = true
                                    break
                                }
                            }

                            // Call holiday logic if data matches today
                            if (hasTodayData) {
                                isTodayHolidayOrSelectedDay()
                            }else{

                            }

                            // Remaining code...
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
            }
        })
        return isTodayMatched
    }
    private fun fetchDataDinas():Boolean {
        var isTodayMatched = false
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val call = perusahaan?.let { pekerja?.let { it1 -> apiService.getDataDinasPekerja(it.nama, it1.nama) } }
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")
                            val today = Calendar.getInstance().time
                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val tanggalBerangkat = parseDate(jsonObject.getString("tanggal_berangkat"))
                                val tanggalPulang = parseDate(jsonObject.getString("tanggal_pulang"))
                                val status = jsonObject.getString("status")

                                // Check if today's date is between tanggal_berangkat and tanggal_pulang
                                if (tanggalBerangkat <= today && today <= tanggalPulang && status == "Accept") {
                                    isTodayMatched = true
                                    keterangan = jsonObject.getString("tujuan")
                                    break // No need to continue checking
                                }
                            }

                            // Remaining code...
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
            }
        })
        return isTodayMatched
    }

    private fun isTodayNationalHoliday(): Boolean {
        val today = Calendar.getInstance()

        // Check if today is in the holidaysMap
        holidaysMap.forEach { (date, description) ->
            if (today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == date.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH)) {
                textView.text = "Today is a national holiday: $description. Enjoy your day!"
                return true
            }
        }
        return false
    }
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, get user's location
            getUserLocation()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get user's location
                getUserLocation()
            } else {
                MotionToast.createToast(
                    requireActivity(),
                    "Absen Failed",
                    "Pls Enabled The Location.",
                    MotionToastStyle.ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    ResourcesCompat.getFont(
                        requireContext(),
                        R.font.ralewaybold
                    )
                )
                codeScanner?.startPreview()
            }
        }
    }
    private fun getUserLocation() {
        val locationManager =  requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            // Get the last known location from the GPS provider
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude

            } else {
                // Handle the case where location is not available
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun getAllSecretKeysFromApi(qrCode: String) {
            val apiUrl = "https://selected-jaguar-presently.ngrok-free.app/api/getAllSecretKeys"
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, apiUrl, null,
            { response ->
                val secretKeysList = mutableListOf<SecretKeyInfo>()
                val perusahaanArray = response.getJSONArray("perusahaan")
                for (i in 0 until perusahaanArray.length()) {
                    val perusahaanObject = perusahaanArray.getJSONObject(i)
                    val namaPerusahaan = perusahaanObject.getString("nama")
                    val secretKey = perusahaanObject.getString("secret_key")
                    val jamMasuk = perusahaanObject.getString("jam_masuk")
                    val jamKeluar = perusahaanObject.getString("jam_keluar")
                    secretKeysList.add(SecretKeyInfo(namaPerusahaan, secretKey, jamMasuk, jamKeluar))
                }
                // Compare QR code with API secret keys
                val isValidQRCode = compareSecretKeys(secretKeysList, qrCode)
                // Handle the result
                if (isValidQRCode) {
                    perusahaan?.let { pekerja?.let { it1 -> Presensi(it, it1) } }
                } else {
                    MotionToast.createToast(requireActivity(), "Error",
                        "QR CODE INVALID",
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(requireContext(), R.font.ralewaybold))
                    codeScanner?.startPreview()
                }
            },
            { error ->
                error.message?.let { Log.d("testing", it) }
            })

        requestQueue.add(jsonObjectRequest)
    }

    private fun compareSecretKeys(apiSecretKeys: List<SecretKeyInfo>, qrCode: String): Boolean {
        for (secretKeyInfo in apiSecretKeys) {
            // Destructure secretKeyInfo to extract fields
            val (namaPerusahaan, secretKey, jamMasuk, jamKeluar) = secretKeyInfo
            Log.d("QRCode", "QR Code: $qrCode, SecretKeys: $secretKey")

            // Compare the QR code with the secret key and company name
            if (secretKey == qrCode && namaPerusahaan == perusahaan?.nama) {
                Toast.makeText(requireContext(), "Valid QR Code for $namaPerusahaan", Toast.LENGTH_SHORT).show()
                return true
            }
        }

        // Invalid QR Code
        Toast.makeText(requireContext(), "Invalid QR Code", Toast.LENGTH_SHORT).show()
        return false
    }

    private fun startTrackingService() {
        trackingIntent.putExtra("perusahaan", perusahaan)
        trackingIntent.putExtra("pekerja", pekerja)
        ContextCompat.startForegroundService(requireContext(), trackingIntent)
    }

    private fun stopTrackingService() {
        requireContext().stopService(trackingIntent)
    }
    // Check and request location permission
        private fun Presensi(perusahaan: Perusahaan, pekerja: Pekerja){
            val url = "https://selected-jaguar-presently.ngrok-free.app/api/Presensi/Absensi"
            Log.d("testing",url)
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendar.time)
            val params = JSONObject()
            val parsedDate: Date = dateFormat.parse(currentDate)
            params.put("nama", pekerja.nama)
            params.put("perusahaan", perusahaan.nama)
            params.put("tanggal", currentDate)
            params.put("jam", currentTime)
            params.put("latitude", latitude)
            params.put("longitude", longitude)
            Log.d("testing",params.toString())
            val request = JsonObjectRequest(
                Request.Method.POST, url, params,
                { response ->
                    Log.d("testing",url)
                    try {
                        val message = response.getString("message")
                        // Process the status and message accordingly
                        when (message) {
                            "Absen Started" -> {
                                val absen = perusahaan.id?.let {
                                    pekerja.id?.let { it1 ->
                                        Absen(
                                            null,
                                            it1,
                                            it,
                                            parsedDate,
                                            currentTime,
                                            null.toString(),
                                            latitude,
                                            longitude)
                                    }
                                }
                                val sharedPreferencesManager = SharedPreferencesManager(requireContext())
                                if (absen != null) {
                                    sharedPreferencesManager.savePresensi(absen)
                                }
                                Log.d("testing3", "Done")
                                val startServiceIntent = Intent(requireActivity(), Tracking::class.java)
                                startServiceIntent.putExtra("perusahaan", perusahaan)
                                startServiceIntent.putExtra("pekerja", pekerja)
                                startTrackingService()
                                requireActivity().runOnUiThread {
                                    val party = Party(
                                        speed = 0f,
                                        maxSpeed = 30f,
                                        damping = 0.9f,
                                        spread = 360,
                                        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                                        position = Position.Relative(0.5, 0.3),
                                        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
                                    )
                                    viewKonfetti.start(party)
                                    MotionToast.createToast(
                                        requireActivity(),
                                            "Absen Started",
                                            "Happy Working",
                                        MotionToastStyle.SUCCESS,
                                        MotionToast.GRAVITY_BOTTOM,
                                        MotionToast.LONG_DURATION,
                                        ResourcesCompat.getFont(
                                            requireContext(),
                                            R.font.ralewaybold
                                        )
                                    )
                                    sendEmailToAllAdmins(
                                        subject = "Employee Check-In Notification",
                                        message = """
                            Notification: Employee Check-In

                            Employee Name: ${pekerja.nama}
                            Company: ${perusahaan.nama}
                            Date: $currentDate
                            Check-in Time: $currentTime
                            Location: [$latitude, $longitude]
                            
                            The employee has checked in. Please monitor attendance and ensure compliance.
                        """.trimIndent()
                                    )
                                    textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                                    textView.setText("Happy Working. \nCome Back When It Already Closed Time")
                                    codeScanner?.stopPreview()
                                    scannerView.visibility = View.GONE
                                    lottie.repeatCount = LottieDrawable.INFINITE
                                    lottie.visibility = View.VISIBLE
                                    lottie.playAnimation()
                                }
                            }

                            "Absen Ended" -> {
                                Log.d("testing2",url)
                                stopTrackingService()
                                requireActivity().runOnUiThread {
                                    val party = Party(
                                        speed = 0f,
                                        maxSpeed = 30f,
                                        damping = 0.9f,
                                        spread = 360,
                                        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                                        position = Position.Relative(0.5, 0.3),
                                        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
                                    )
                                    viewKonfetti.start(party)
                                    val sharedPreferencesManager = SharedPreferencesManager(requireContext())
                                    sharedPreferencesManager.removePresensi()
                                    MotionToast.createToast(
                                        requireActivity(),
                                        "Absen Completed",
                                        "Have A Nice Day",
                                        MotionToastStyle.SUCCESS,
                                        MotionToast.GRAVITY_BOTTOM,
                                        MotionToast.LONG_DURATION,
                                        ResourcesCompat.getFont(
                                            requireContext(),
                                            R.font.ralewaybold
                                        )
                                    )
                                    sendEmailToAllAdmins(
                                        subject = "Employee Check-Out Notification",
                                        message = """
                            Notification: Employee Check-Out

                            Employee Name: ${pekerja.nama}
                            Company: ${perusahaan.nama}
                            Date: $currentDate
                            Check-out Time: $currentTime
                            Location: [$latitude, $longitude]
                            
                            The employee has checked out. Please review attendance records.
                        """.trimIndent()
                                    )
                                    textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
                                    textView.setText("Thank You For Your Hard Work Today ${pekerja.nama}")
                                    codeScanner?.stopPreview()
                                    scannerView.visibility = View.GONE
                                    lottie.setAnimation(R.raw.done)
                                    lottie.repeatCount = LottieDrawable.INFINITE
                                    lottie.visibility = View.VISIBLE
                                    lottie.playAnimation() // Start the animation
                                }
                            }
                        else -> {
                            Log.d("testing1",url)
                            requireActivity().runOnUiThread {
                                MotionToast.createToast(
                                    requireActivity(),
                                    "Absen Failed",
                                    "You can only absen within 15 minutes of the scheduled time. Please try again.",
                                    MotionToastStyle.ERROR,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.LONG_DURATION,
                                    ResourcesCompat.getFont(
                                        requireContext(),
                                        R.font.ralewaybold
                                    )
                                )
                                codeScanner?.startPreview()
                            }
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Log.d("testing",url)
                    codeScanner?.startPreview()
                }
            },
            { error ->
                // Handle error
                error.printStackTrace()
                codeScanner?.startPreview()
            }
        )
        requestQueue.add(request)
    }
    private fun sendEmailToAllAdmins(subject: String, message: String) {
        adminList?.forEach { admin ->
            val receiverEmail = admin.email
            EmailSender.sendEmail(receiverEmail, subject, message)
        }
    }
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
    private fun getBundle() {
        val arguments = arguments
        if (arguments != null) {
            perusahaan = arguments.getParcelable("perusahaan")
            pekerja = arguments.getParcelable("user")
            selectedDays.clear()
            val holiday = perusahaan?.holiday
            if (holiday != null) {
                selectedDays.addAll(holiday.split(",\\s*".toRegex()).map { it.trim() })
            }
            Log.d("Holiday",perusahaan.toString())
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }
}
