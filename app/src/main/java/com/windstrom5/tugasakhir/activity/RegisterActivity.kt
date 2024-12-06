package com.windstrom5.tugasakhir.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MultiAutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.ferfalk.simplesearchview.SimpleTextWatcher
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.teamwork.autocomplete.MultiAutoComplete
import com.teamwork.autocomplete.tokenizer.PrefixTokenizer
import com.teamwork.autocomplete.view.MultiAutoCompleteEditText
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.databinding.ActivityRegisterBinding
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.perusahaancreate
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.io.File
import java.io.IOException
import java.security.MessageDigest
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
    private lateinit var autoCompleteTextView: AutoCompleteTextView
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
    private val selectedItems = mutableSetOf<String>() // To keep track of selected items
    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 123
    }
    private val selectedDays = mutableListOf<String>() // List to store selected days
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestQueue = Volley.newRequestQueue(this)
        location = binding.selectLocationButton
        TINamaPerusahaan = binding.textInputPerusahaan
        TIJamkeluar = binding.textInputkeluar
        autoCompleteTextView = binding.acholiday
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.holiday_holiday_options,
            android.R.layout.simple_dropdown_item_1line
        )

//        autoCompleteTextView.setAdapter(adapter)
//        val tokenizer: MultiAutoCompleteTextView.Tokenizer = PrefixTokenizer(',')
//        autoCompleteTextView.setTokenizer(tokenizer)
//        autoCompleteTextView.addTextChangedListener(object : SimpleTextWatcher() {
//            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
//                val currentInput = s.toString().trim()
//                val suggestions = holidayholidayOptions.filter { option ->
//                    option.contains(currentInput, ignoreCase = true) && !selectedItems.contains(option)
//                }
//                val newAdapter = ArrayAdapter(this@RegisterActivity, android.R.layout.simple_dropdown_item_1line, suggestions)
//                autoCompleteTextView.setAdapter(newAdapter)
//            }
//        })

        // Optionally, set a hint to indicate what the user should do
        autoCompleteTextView.hint = "Hari Libur"
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

        autoCompleteTextView.setOnClickListener {
            val holidayOptions = resources.getStringArray(R.array.holiday_holiday_options)
            val checkedItems = BooleanArray(holidayOptions.size)
            showDaySelectionDialog(holidayOptions, checkedItems)
        }

        location.setOnClickListener {
//            if (edNamaPerusahaan.text.isNotEmpty()) {
            val intent = Intent(this, MapActivity::class.java)
            val bundle = Bundle()
            bundle.putString("namaPerusahaan", TINamaPerusahaan.editText?.text.toString())
            bundle.putString("openhour", TIJammasuk.editText?.text.toString())
            bundle.putString("closehour", TIJamkeluar.editText?.text.toString())
            bundle.putString("holiday", autoCompleteTextView.text.toString())
            bundle.putString("category", "register")
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
            if (TINamaPerusahaan == null || TIJammasuk == null || TIJamkeluar == null || information.visibility == View.GONE || autoCompleteTextView.text.isNullOrEmpty()) {
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
                secretKey,
                autoCompleteTextView.text.toString()
            )
            val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
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
                            var perusahaanExists = false

                            for (i in 0 until perusahaanArray.length()) {
                                val perusahaanObj = perusahaanArray.getJSONObject(i)
                                val perusahaanNama = perusahaanObj.getString("nama").trim()

                                // Compare the input nama with the perusahaan nama
                                if (TINamaPerusahaan.editText?.text.toString().equals(perusahaanNama, ignoreCase = true)) {
                                    perusahaanExists = true
                                    break
                                }
                            }

                            if (perusahaanExists) {
                                // Handle case where perusahaan with the same name already exists
                                runOnUiThread {
                                    setLoading(false)
                                    MotionToast.createToast(
                                        this@RegisterActivity,
                                        "Error",
                                        "Perusahaan Sudah Ada",
                                        MotionToastStyle.ERROR,
                                        MotionToast.GRAVITY_BOTTOM,
                                        MotionToast.LONG_DURATION,
                                        ResourcesCompat.getFont(this@RegisterActivity, R.font.ralewaybold)
                                    )
                                }
                            }else{
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
                    when (t) {
                        is IOException -> {
                            // No internet connection on the device
                            Toast.makeText(
                                this@RegisterActivity,
                                "No internet connection. Please check your network and try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is HttpException -> {
                            // Server is reachable, but there’s an issue on the server
                            val statusCode = t.code()
                            Toast.makeText(
                                this@RegisterActivity,
                                "Server error (code: $statusCode). Please try again later.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            // General error
                            Toast.makeText(
                                this@RegisterActivity,
                                "Request failed: ${t.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    setLoading(false)
                }
            })

        }
    }
    private fun showDaySelectionDialog(daysArray: Array<String>, checkedItems: BooleanArray) {
        // Initialize the selectedDaysList based on the current value in AutoCompleteTextView
        val currentText = autoCompleteTextView.text.toString()
        val selectedDaysList = if (currentText.isNotBlank()) {
            currentText.split(", ").map { it.trim() }.toMutableList()
        } else {
            mutableListOf()
        }

        // Initialize checkedItems based on selectedDaysList
        for (i in daysArray.indices) {
            checkedItems[i] = selectedDaysList.contains(daysArray[i])
        }

        AlertDialog.Builder(this)
            .setTitle("Pilih Hari Libur")
            .setMultiChoiceItems(daysArray, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    if (daysArray[which].isNotBlank()) {
                        selectedDaysList.add(daysArray[which]) // Add day to the list if checked
                    }
                } else {
                    selectedDaysList.remove(daysArray[which]) // Remove day if unchecked
                }
            }
            .setPositiveButton("OK") { dialog, _ ->
                // Format the list properly without leading or trailing commas
                val formattedText = selectedDaysList.joinToString(", ").trim()
                autoCompleteTextView.setText(formattedText) // Display selected days
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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

    // Function to generate a random string and back it with MD5 hashing
    private fun generateRandomString(): String {
        // Fetch existing MD5-hashed secret keys from the server
        val existingSecretKeys = getSecretKeysFromApi()

        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
        val random = SecureRandom()

        while (true) {
            val randomString = StringBuilder()
            for (i in 0 until 20) {
                val randomChar = charset[random.nextInt(charset.length)]
                randomString.append(randomChar)
            }

            // Compute the MD5 hash of the generated string
            val randomStringMd5 = md5(randomString.toString())

            // Check if the MD5 hash already exists in the list
            if (!existingSecretKeys.contains(randomStringMd5)) {
                return randomString.toString() // Return the raw string, but keep the hash on the server
            }
        }
    }

    // Function to fetch secret keys (already hashed with MD5) from the API
    private fun getSecretKeysFromApi(): List<String> {
        val apiUrl = "https://selected-jaguar-presently.ngrok-free.app/api/GetPerusahaan"
        val secretKeysList = mutableListOf<String>()

        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, apiUrl, null,
            { response ->
                // Parse the JSON array and extract secret keys (assumed to be hashed with MD5 already)
                for (i in 0 until response.length()) {
                    val secretKeyHash = response.getString(i)
                    secretKeysList.add(secretKeyHash) // These are the MD5 hashes
                }
            },
            { error ->
                // Handle error cases
            })

        // Add the request to the RequestQueue
        requestQueue.add(jsonArrayRequest)

        return secretKeysList
    }

    // Function to compute MD5 hash
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
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
//        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
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
        return RequestBody.create("text/plain".toMediaTypeOrNull(), value)
    }

    private val holidayholidayMapping = mapOf(
        "Nasional" to "National Holiday",
        "Senin" to "Monday",
        "Selasa" to "Tuesday",
        "Rabu" to "Wednesday",
        "Kamis" to "Thursday",
        "Jumat" to "Friday",
        "Sabtu" to "Saturday",
        "Minggu" to "Sunday"
    )

    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                val namaPerusahaan = it.getString("namaPerusahaan") ?: ""
                val openHours = it.getString("openhour") ?: ""
                val closeHours = it.getString("closehour") ?: ""
                val latitude = it.getDouble("latitude")
                val longitude = it.getDouble("longitude")
                val address = it.getString("address")
                val holiday = it.getString("holiday")
                if (address != null) {
                    information.visibility = View.VISIBLE
                    tvaddress.text = address.toString()
                    Tvlatitude.text = latitude.toString()
                    Tvlongitude.text = longitude.toString()
                    edNamaPerusahaan.setText(namaPerusahaan)
                    TIJamkeluar.editText?.setText(closeHours)
                    TIJammasuk.editText?.setText(openHours)
                    autoCompleteTextView.setText(holiday, false)
                }
            }
        } else {
            Log.d("Error", "Bundle Not Found")
        }
    }
}