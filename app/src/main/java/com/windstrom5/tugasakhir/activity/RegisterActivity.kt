package com.windstrom5.tugasakhir.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.databinding.ActivityRegisterBinding
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.perusahaancreate
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.File
import java.security.SecureRandom
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import retrofit2.Response as RetrofitResponse

class RegisterActivity : AppCompatActivity() {
    private lateinit var location: Button
    private lateinit var TINamaPerusahaan: TextInputLayout
    private lateinit var TIJammasuk: TextInputLayout
    private lateinit var TIJamkeluar: TextInputLayout
    private lateinit var tvaddress: TextView
    private lateinit var Tvlongitude: TextView
    private lateinit var requestQueue: RequestQueue
    private lateinit var Tvlatitude: TextView
    private lateinit var edNamaPerusahaan: EditText
    private lateinit var selectedFileName: TextView
    private var perusahaan: Perusahaan? = null
    private var bundle: Bundle? = null
    private lateinit var information: CardView
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var upload: Button
    private lateinit var createnow: Button
    private var selectedImageByteArray: ByteArray? = null
    private var selectedFile: File? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewProgress: TextView
    private lateinit var textViewCustom: TextView
    private val boundary = "*****"
    private lateinit var imageView: ImageView
    private lateinit var loading: LinearLayout
    private lateinit var path: String

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestQueue = Volley.newRequestQueue(this)
        location = binding.selectLocationButton
        TINamaPerusahaan = binding.textInputPerusahaan
        TIJamkeluar = binding.textInputkeluar
        loading = findViewById(R.id.layout_loading)
        TIJammasuk = binding.textInputMasuk
        imageView = binding.imageView
        edNamaPerusahaan = binding.editTextPerusahaan
        tvaddress = binding.tvAddress
        selectedFileName = binding.selectedFileName
        Tvlongitude = binding.tvlongitude
        Tvlatitude = binding.tvLatitude
        information = binding.information
        upload = binding.uploadfile
        createnow = binding.cirLoginButton
        getBundle()
        location.setOnClickListener {
//            if (edNamaPerusahaan.text.isNotEmpty()) {
            val intent = Intent(this, MapActivity::class.java)
            val bundle = Bundle()
            bundle.putString("namaPerusahaan", TINamaPerusahaan.editText?.text.toString())
            bundle.putString("openhour", TIJammasuk.editText?.text.toString())
            bundle.putString("closehour", TIJamkeluar.editText?.text.toString())
            bundle.putString("category", "edit")
            intent.putExtra("data", bundle)
            startActivity(intent)
//            } else {
//                startActivity(Intent(this, MapActivity::class.java))
//            }
        }
        TIJammasuk.setEndIconOnClickListener {
            showTimePicker(TIJammasuk)
        }
        TIJamkeluar.setEndIconOnClickListener {
            showTimePicker(TIJamkeluar)
        }
        upload.setOnClickListener {
            pickImageFromGallery()
        }
        createnow.setOnClickListener {
            setLoading(true)
            if (TINamaPerusahaan == null || TIJammasuk == null || TIJamkeluar == null || information.visibility == View.GONE) {
                MotionToast.createToast(
                    this@RegisterActivity, "Error",
                    "Ada Form Yang belum Terisi",
                    MotionToastStyle.ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    ResourcesCompat.getFont(this@RegisterActivity, R.font.ralewaybold)
                )
            }
            val secretKey = generateRandomString()
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, 1)
            val futureDate = calendar.time
            val sqlDate = java.sql.Date(futureDate.time)
            val register = perusahaancreate(
                TINamaPerusahaan.editText?.text.toString(),
                Tvlatitude.text.toString().toDouble(),
                Tvlongitude.text.toString().toDouble(),
                stringToSqlTime(TIJammasuk.editText?.text.toString()),
                stringToSqlTime(TIJamkeluar.editText?.text.toString()),
                sqlDate,
                selectedFile,
                secretKey
            )
            val url = "http://192.168.1.6:8000/api/"
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)
            val call = apiService.getPerusahaan()

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: RetrofitResponse<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        val json = responseBody?.let { it1 -> JSONObject(it1) }
                        val perusahaanArray = json?.optJSONArray("perusahaan")
                        if (perusahaanArray != null && perusahaanArray.length() > 0) {
                            // Handle case where perusahaan array is not empty
                            runOnUiThread {
                                setLoading(false)
                                MotionToast.createToast(
                                    this@RegisterActivity, "Error",
                                    "Perusahaan Sudah Ada",
                                    MotionToastStyle.ERROR,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.LONG_DURATION,
                                    ResourcesCompat.getFont(
                                        this@RegisterActivity,
                                        R.font.ralewaybold
                                    )
                                )
                            }
                        } else {
                            // Handle case where perusahaan array is empty
                            runOnUiThread {
                                setLoading(false)
                                val intent =
                                    Intent(this@RegisterActivity, RegisterAdminActivity::class.java)
                                val bundle = Bundle()
                                bundle.putParcelable("perusahaan", register)
                                Log.d("perusahaan",register.toString())
                                intent.putExtra("data", bundle)
                                startActivity(intent)
                            }
                        }
                    } else {
                        // Handle unsuccessful response
                        setLoading(false)
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    // Handle failure
                    setLoading(false)
                }
            })

        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            loading!!.visibility = View.VISIBLE
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            loading!!.visibility = View.INVISIBLE
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
    }

    private fun generateRandomString(): String {
        // Fetch existing secret keys from the server
        val existingSecretKeys = getSecretKeysFromApi()

        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
        val random = SecureRandom()

        while (true) {
            val randomString = StringBuilder()
            for (i in 0 until 20) {
                val randomChar = charset[random.nextInt(charset.length)]
                randomString.append(randomChar)
            }

            // Check if the generated key already exists
            if (!existingSecretKeys.contains(randomString.toString())) {
                return randomString.toString()
            }
        }
    }

    private fun getSecretKeysFromApi(): List<String> {
        val apiUrl = "http://192.168.1.6:8000/api/GetPerusahaan"

        val secretKeysList = mutableListOf<String>()

        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, apiUrl, null,
            { response ->
                // Parse the JSON array and extract secret keys
                for (i in 0 until response.length()) {
                    val secretKey = response.getString(i)
                    secretKeysList.add(secretKey)
                }
            },
            { error ->
                // Handle error cases
            })

        // Add the request to the RequestQueue
        requestQueue.add(jsonArrayRequest)

        return secretKeysList
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { imageUri ->
                // Get the real path from the URI
                val realPath = getRealPathFromUri(imageUri)
                if (realPath != null) {
                    selectedFileName.text = File(realPath).name
                    selectedFile = File(realPath)
                    imageView.visibility = View.VISIBLE
                    Glide.with(this)
                        .load(imageUri) // Load the image using the URI
                        .into(imageView) // Set it into the ImageView
                } else {
                    selectedFileName.text = "Failed to get real path"
                }
            }
        }
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            return it.getString(columnIndex)
        }
        return null
    }

    private fun showTimePicker(textInputLayout: TextInputLayout) {
        val calendar = Calendar.getInstance()

        val timePicker = MaterialTimePicker.Builder()
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formattedTime = timeFormat.format(calendar.time)

            textInputLayout.editText?.setText(formattedTime)
        }

        timePicker.show(supportFragmentManager, "timePicker")
    }

    private fun stringToSqlTime(timeString: String): Time {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = dateFormat.parse(timeString)
        return Time(date.time)
    }

//    private fun makeApiRequest(secretKey: String) {
//        val url = "http://192.168.1.6:8000/api/"
//
//        val retrofit = Retrofit.Builder()
//            .baseUrl(url)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        val apiService = retrofit.create(ApiService::class.java)
//
//        // Convert values to RequestBody
//        val nama = createPartFromString(TINamaPerusahaan.editText?.text.toString())
//        val latitude = createPartFromString(Tvlatitude.text.toString().toDouble().toString())
//        val longitude = createPartFromString(Tvlongitude.text.toString().toDouble().toString())
//        val jamMasuk = createPartFromString(stringToSqlTime(TIJammasuk.editText?.text.toString()).toString())
//        val jamKeluar = createPartFromString(stringToSqlTime(TIJamkeluar.editText?.text.toString()).toString())
//        val batasAktif = createPartFromString(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time))
//        val secretKeyPart = createPartFromString(secretKey)
//
//        val logoFile = selectedFile
//        val requestFile = RequestBody.create(MediaType.parse("image/*"), logoFile)
//        val logoPart = MultipartBody.Part.createFormData("logo", logoFile.name, requestFile)
//
//        // Make the API call
//        val call = apiService.uploadPerusahaan(nama, latitude, longitude, jamMasuk, jamKeluar, batasAktif, secretKeyPart, logoPart)
//
//        // Execute the call asynchronously
//        call.enqueue(object : Callback<ApiResponse> {
//            override fun onResponse(call: Call<ApiResponse>, response: RetrofitResponse<ApiResponse>) {
//                if (response.isSuccessful) {
//                    val apiResponse = response.body()
//                    Log.d("ApiResponse", "Status: ${apiResponse?.status}, Message: ${apiResponse?.message}")
//                    Log.d("path", " ${apiResponse?.profile_path}")
//                    // Assuming path is a global variable
//                    val path = apiResponse?.profile_path ?: ""
//                    val id_perusahaan = apiResponse?.id ?: 0
//                    // Continue with other actions after API response
//                    val calendar = Calendar.getInstance()
//                    calendar.add(Calendar.YEAR, 1)
//                    val futureDate = calendar.time
//                    val sqlDate = java.sql.Date(futureDate.time)
//
//                    perusahaan = Perusahaan(
//                        id_perusahaan,
//                        TINamaPerusahaan.editText?.text.toString(),
//                        Tvlatitude.text.toString().toDouble(),
//                        Tvlongitude.text.toString().toDouble(),
//                        stringToSqlTime(TIJammasuk.editText?.text.toString()),
//                        stringToSqlTime(TIJamkeluar.editText?.text.toString()),
//                        sqlDate,
//                        path,
//                        secretKey
//                    )
//                    setLoading(false)
//                    Log.d("Perusahaan", perusahaan?.toString() ?: "Perusahaan is null")
//                    val intent = Intent(this@RegisterActivity, RegisterAdminActivity::class.java)
//                    val bundle = Bundle()
//                    bundle.putParcelable("perusahaan", perusahaan)
//                    intent.putExtra("data", bundle)
//                    startActivity(intent)
//                } else {
//                    Log.e("ApiResponse", "Error: ${response.code()}")
//                }
//            }
//
//            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
//                setLoading(false)
//                Log.e("ApiResponse", "Request failed: ${t.message}")
//            }
//        })
//    }

    private fun createPartFromString(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }


    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                val namaPerusahaan = it.getString("namaPerusahaan") ?: ""
                val openHours = it.getString("openhour") ?: ""
                val closeHours = it.getString("closehour") ?: ""
                val latitude = it.getDouble("latitude")
                Log.d("Editor", latitude.toString())
                val longitude = it.getDouble("longitude")
                val address = it.getString("address")
                if (address != null) {
                    information.visibility = View.VISIBLE
                    tvaddress.text = address.toString()
                    Tvlatitude.text = latitude.toString()
                    Tvlongitude.text = longitude.toString()
                    edNamaPerusahaan.setText(namaPerusahaan)
                    TIJamkeluar.editText?.setText(closeHours)
                    TIJammasuk.editText?.setText(openHours)
                }
            }
        } else {
            Log.d("Error", "Bundle Not Found")
        }
    }
}