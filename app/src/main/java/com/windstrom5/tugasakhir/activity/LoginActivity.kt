package com.windstrom5.tugasakhir.activity

//import com.google.firebase.FirebaseApp
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.safetynet.SafetyNetClient
import com.google.android.gms.safetynet.SafetyNetStatusCodes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.saadahmedev.popupdialog.PopupDialog
import com.windstrom5.tugasakhir.BuildConfig
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.connection.SharedPreferencesManager
import com.windstrom5.tugasakhir.databinding.ActivityLoginBinding
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit.*
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LoginActivity : AppCompatActivity() {
//    private lateinit var textInputPerusahaan: TextInputLayout
//    private lateinit var editTextPerusahaan: AutoCompleteTextView
    private lateinit var textInputEmail: TextInputLayout
//    private lateinit var editTextEmail: EditText
    private lateinit var textInputPassword: TextInputLayout
//    private lateinit var editTextPassword: EditText
    private lateinit var popupWindow: PopupWindow
    private lateinit var loading : LinearLayout
    private lateinit var binding: ActivityLoginBinding
    private lateinit var login : CircularProgressButton
//    private lateinit var acperusahaan : AutoCompleteTextView
//    private val selectedPerusahaanId = null
    private lateinit var register : TextView
    private lateinit var forgotPassword:TextView
    private lateinit var safetyNetClient: SafetyNetClient
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            updateLoginButtonState()
        }
    }
    private lateinit var captchaCheckBox: CheckBox
    private lateinit var captchaProgressBar: ProgressBar
//    private var perusahaanList: List<Perusahaan> = emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        textInputPerusahaan = binding.textInputperusahaan
//        editTextPerusahaan = binding.ACperusahaan
        textInputEmail = binding.textInputEmail
//        editTextEmail = binding.editTextEmail
        textInputPassword = binding.textInputPassword
//        editTextPassword = binding.editTextPassword
//        captchaCheckBox = binding.captchaCheckBox
//        captchaProgressBar = binding.captchaProgressBar

        safetyNetClient = SafetyNet.getClient(this)
        login = binding.cirLoginButton
        forgotPassword = binding.textViewForgotPassword
        register = binding.createPerusahan
        login.isEnabled = false
        loading = findViewById(R.id.layout_loading)
//        perusahaanList = (intent.getSerializableExtra("perusahaanList") as? ArrayList<Perusahaan>)!!
//        setUpAutoCompleteTextView(perusahaanList)
//        fetchDataFromApi()
//        val webView = findViewById<WebView>(R.id.recaptchaWebView)
//        webView.settings.javaScriptEnabled = true

        // Load the reCAPTCHA widget URL
//        webView.loadUrl("https://www.google.com/recaptcha/api2/anchor?ar=1&k=6LfHj9kpAAAAAC9wLkrth3mhf7Kbwh2TypdXI1i4&co=aHR0cHM6Ly93d3cuZ29vZ2xlLmNvbTo0NDM.&hl=en&v=v1658419713836&size=normal&cb=abcdefghijk")
        textInputEmail.editText?.addTextChangedListener(textWatcher)
        textInputPassword.editText?.addTextChangedListener(textWatcher)
//        editTextPerusahaan.addTextChangedListener(textWatcher)
        forgotPassword.setOnClickListener{
            val intent = Intent(this,ForgotActivity::class.java)
            startActivity(intent)
        }
//        captchaCheckBox.setOnCheckedChangeListener { _, isChecked ->
//            if (isChecked) {
//                showCaptchaDialog()
//            }
//        }
        login.setOnClickListener{
            login.startAnimation()
            //setLoading(true)
//            showCaptchaDialog()
            login(textInputEmail.editText?.text.toString(),
                textInputPassword.editText?.text.toString())
        }
        register.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    private fun showCaptchaDialog() {
//        captchaProgressBar.visibility = View.VISIBLE
        val clientId = BuildConfig.Chapta_Key
        val clientSecret = BuildConfig.Chapta_Secret
        safetyNetClient.verifyWithRecaptcha(clientId)
            .addOnSuccessListener(this) { response ->
                handleVerificationSuccess(response.tokenResult)
            }
            .addOnFailureListener(this) { e ->
                handleVerificationFailure(e)
            }
    }

    private fun handleVerificationSuccess(tokenResult: String?) {
        // Verify the token with your server-side logic using chapta_secret
        if (tokenResult != null) {
            // Send token to your server for verification
            // Handle server's response accordingly
            PopupDialog.getInstance(this@LoginActivity)
                .statusDialogBuilder()
                .createSuccessDialog()
                .setHeading("Chapta Success")
                .setDescription("Processing to Login")
                .build(Dialog::dismiss)
                .show();
            login.revertAnimation()
            login(textInputEmail.editText?.text.toString(),
                textInputPassword.editText?.text.toString())
        }
    }

    private fun handleVerificationFailure(exception: Exception) {
        if (exception is ApiException) {
            val statusCode = exception.statusCode
            // Handle specific error codes
            when (statusCode) {
                SafetyNetStatusCodes.RECAPTCHA_INVALID_SITEKEY -> {
                    // Handle invalid key error
                    Log.d("error","Invalid reCAPTCHA API key")
                }
                SafetyNetStatusCodes.NETWORK_ERROR -> {
                    // Handle network error
                    Log.d("error","Network error. Please check your internet connection.")
                }
                else -> {
                    // Handle other errors
                    Log.d("error","CAPTCHA verification failed. Error: ${statusCode}")
                }
            }
        } else {
            // Handle other types of exceptions
            Log.d("error","CAPTCHA verification failed: ${exception.message}")
        }
    }
//    private fun fetchDataFromApi() {
//        val url = "http://192.168.1.6:8000/api/GetPerusahaan"
//        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
//            { response ->
//                val perusahaanArray = response.getJSONArray("perusahaan")
//                val newPerusahaanList = mutableListOf<Perusahaan>()
//                for (i in 0 until perusahaanArray.length()) {
//                    val perusahaanObject = perusahaanArray.getJSONObject(i)
//                    val id = perusahaanObject.getInt("id")
//                    val nama = perusahaanObject.getString("nama")
//                    val latitude = perusahaanObject.getDouble("latitude")
//                    val longitude = perusahaanObject.getDouble("longitude")
//                    val jam_masukStr = perusahaanObject.getString("jam_masuk")
//                    val jam_keluarStr = perusahaanObject.getString("jam_keluar")
//                    val jam_masuk = convertStringToTime(jam_masukStr)
//                    val jam_keluar = convertStringToTime(jam_keluarStr)
//                    val batasAktif = perusahaanObject.getString("batas_aktif")
//                    val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                    val javaUtilDate = dateParser.parse(batasAktif)
//
//                    // Convert java.util.Date to java.sql.Date
//                    val sqlDate = java.sql.Date(javaUtilDate.time)
//                    val logo = perusahaanObject.getString("logo")
//                    val secretKey = perusahaanObject.getString("secret_key")
//                    val perusahaan = Perusahaan(id,nama, latitude, longitude, jam_masuk,jam_keluar,sqlDate, logo, secretKey)
//                    newPerusahaanList.add(perusahaan)
//                }
////                perusahaanList = newPerusahaanList
////                setUpAutoCompleteTextView(perusahaanList)
//            },
//            { error ->
//                error.printStackTrace()
//            })
//
//        Volley.newRequestQueue(this).add(jsonObjectRequest)
//    }
    @SuppressLint("SimpleDateFormat")
    fun convertStringToTime(timeStr: String): Time {
        val sdf = SimpleDateFormat("HH:mm:ss")
        val date: Date = sdf.parse(timeStr)
        return Time(date.time)
    }
    private fun setLoading(isLoading:Boolean){
        if(isLoading){
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            loading.visibility = View.VISIBLE
        }else{
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            loading.visibility = View.INVISIBLE
        }
    }
//    private fun setUpAutoCompleteTextView(perusahaanList: List<Perusahaan>) {
//        val autoCompleteTextView: AutoCompleteTextView = findViewById(R.id.ACperusahaan)
//        val adapter = ArrayAdapter(
//            this,
//            android.R.layout.simple_dropdown_item_1line,
//            perusahaanList.map { it.nama }
//        )
//        autoCompleteTextView.setAdapter(adapter)
//        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
//            val selectedPerusahaan = perusahaanList[position]
//        }
//        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                // Not used
//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                // Not used
//            }
//
//            override fun afterTextChanged(s: Editable?) {
//                // Handle text changes
//                val searchText = s.toString().trim()
//
//                // Filter the list based on the current search text
//                val filteredList = perusahaanList.filter { it.nama.contains(searchText, true) }
//
//                // Update the adapter with the filtered list
//                val filteredAdapter = ArrayAdapter(
//                    this@LoginActivity,
//                    android.R.layout.simple_dropdown_item_1line,
//                    filteredList.map { it.nama }
//                )
//                autoCompleteTextView.setAdapter(filteredAdapter)
//            }
//        })
//    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to exit the app?")
            .setPositiveButton("Yes") { _, _ ->
                super.onBackPressed()
                finishAffinity() // Close all activities in the task
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateLoginButtonState() {
//        val isPerusahaanFilled = editTextPerusahaan.text.isNotBlank()
        val isEmailFilled = textInputEmail.editText?.text?.isNotBlank()
        val isPasswordFilled = textInputPassword.editText?.text?.isNotBlank()

        val isAllFieldsFilled = isEmailFilled == true && isPasswordFilled == true
        login.isEnabled = isAllFieldsFilled
    }
    private fun login(email: String, password: String) {
        val url = "http://192.168.1.6:8000/api/login"
//        val checkBoxRememberMe = findViewById<CheckBox>(R.id.checkBoxRememberMe)
//        val rememberMeChecked = checkBoxRememberMe.isChecked
        val sharedPreferencesManager = SharedPreferencesManager(this)
//        val matchingPerusahaan = perusahaanList.find { it.nama == namaPerusahaan }
//        Log.d("Perusahaan", perusahaanList.toString())
//        if (matchingPerusahaan != null) {
            val jsonParams = JSONObject()
            jsonParams.put("email", email)
            jsonParams.put("password", password)
            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonParams,
                { response ->
                    try {
                        val perusahaanObject= response.getJSONObject("perusahaan")
                        val user = response.getJSONObject("user")
                        val role = response.getString("Role")
                        val id = perusahaanObject.getInt("id")
                        val nama = perusahaanObject.getString("nama")
                        val latitude = perusahaanObject.getDouble("latitude")
                        val longitude = perusahaanObject.getDouble("longitude")
                        val jam_masukStr = perusahaanObject.getString("jam_masuk")
                        val jam_keluarStr = perusahaanObject.getString("jam_keluar")
                        val jam_masuk = convertStringToTime(jam_masukStr)
                        val jam_keluar = convertStringToTime(jam_keluarStr)
                        val batasAktif = perusahaanObject.getString("batas_aktif")
                        val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val javaUtilDate = dateParser.parse(batasAktif)
                        val sqlDate = java.sql.Date(javaUtilDate.time)
                        val logo = perusahaanObject.getString("logo")
                        val secretKey = perusahaanObject.getString("secret_key")
                        sharedPreferencesManager.clearUserData()
                        Log.d("Role",role)
                        if (role == "Admin") {
                            val admin = Admin(
                                user.getInt("id"),
                                user.getInt("id_perusahaan"),
                                user.getString("email"),
                                user.getString("password"),
                                user.getString("nama"),
                                parseDate(user.getString("tanggal_lahir")),
                                user.getString("profile")
                            )
                            val perusahaan = Perusahaan(
                                id,
                                nama,
                                latitude,
                                longitude,
                                jam_masuk,
                                jam_keluar,
                                sqlDate,
                                logo,
                                secretKey
                            )
                            setLoading(false)
                            val vectorDrawable = ContextCompat.getDrawable(this@LoginActivity, R.drawable.done_bitmap)
                            val bitmap = vectorDrawable?.let { vectorToBitmap(it) }
                            login.doneLoadingAnimation(Color.parseColor("#AAFF00"), bitmap)
//                            if (rememberMeChecked) {
                                sharedPreferencesManager.saveAdmin(admin)
                                sharedPreferencesManager.savePerusahaan(perusahaan)
//                            }
                            val intent = Intent(this, AdminActivity::class.java)
                            val userBundle = Bundle()
                            userBundle.putParcelable("user", admin)
                            userBundle.putParcelable("perusahaan", perusahaan)
                            intent.putExtra("data", userBundle)
                            startActivity(intent)
                        } else {
                            val vectorDrawable = ContextCompat.getDrawable(this@LoginActivity, R.drawable.done_bitmap)
                            val bitmap = vectorDrawable?.let { vectorToBitmap(it) }
                            login.doneLoadingAnimation(Color.parseColor("#AAFF00"), bitmap)
                            val perusahaan = Perusahaan(
                                id,
                                nama,
                                latitude,
                                longitude,
                                jam_masuk,
                                jam_keluar,
                                sqlDate,
                                logo,
                                secretKey
                            )
                            val pekerja = Pekerja(
                                user.getInt("id"),
                                user.getInt("id_perusahaan"),
                                user.getString("email"),
                                user.getString("password"),
                                user.getString("nama"),
                                parseDate(user.getString("tanggal_lahir")),
                                user.getString("profile")
                            )
                            val url2 = "http://192.168.1.6:8000/api/"
                            val retrofit = Builder()
                                .baseUrl(url2)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                            val apiService = retrofit.create(ApiService::class.java)
                            val call = apiService.getDataAbsenPekerja(perusahaan.nama,pekerja.nama)
                            call.enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { responseBody ->
                                            try {
                                                val jsonResponse = responseBody.string()
                                                val jsonArray = JSONArray(jsonResponse)
                                                val today = java.sql.Date(System.currentTimeMillis())
                                                if (jsonArray.length() == 0) {
                                                    // No data in JSON array
                                                    sharedPreferencesManager.savePekerja(pekerja)
                                                    sharedPreferencesManager.savePerusahaan(perusahaan)
                                                    val intent = Intent(this@LoginActivity, UserActivity::class.java)
                                                    val userBundle = Bundle()
                                                    userBundle.putParcelable("user", pekerja)
                                                    userBundle.putParcelable("perusahaan", perusahaan)
                                                    intent.putExtra("data", userBundle)
                                                    startActivity(intent)
                                                } else {
                                                    for (i in 0 until jsonArray.length()) {
                                                        val jsonObject = jsonArray.getJSONObject(i)
                                                        val tanggalString =
                                                            jsonObject.getString("tanggal")
                                                        val tanggal =
                                                            java.sql.Date.valueOf(tanggalString)
                                                        Log.d("tanggal", tanggalString)
                                                        Log.d("tanggal", today.toString())
                                                        if (tanggalString == today.toString()) {
                                                            if (jsonObject.has("jam_keluar")) {
                                                                sharedPreferencesManager.savePekerja(
                                                                    pekerja
                                                                )
                                                                sharedPreferencesManager.savePerusahaan(
                                                                    perusahaan
                                                                )
                                                                val intent = Intent(
                                                                    this@LoginActivity,
                                                                    UserActivity::class.java
                                                                )
                                                                val userBundle = Bundle()
                                                                userBundle.putParcelable(
                                                                    "user",
                                                                    pekerja
                                                                )
                                                                userBundle.putParcelable(
                                                                    "perusahaan",
                                                                    perusahaan
                                                                )
                                                                intent.putExtra("data", userBundle)
                                                                startActivity(intent)
                                                            } else {
                                                                PopupDialog.getInstance(this@LoginActivity)
                                                                    .statusDialogBuilder()
                                                                    .createErrorDialog()
                                                                    .setHeading("Cannot Login")
                                                                    .setDescription("User is working right now")
                                                                    .build(Dialog::dismiss)
                                                                    .show();
                                                                login.revertAnimation()
                                                            }
                                                        } else {
                                                            Log.d("AMM", "AMM")
                                                            sharedPreferencesManager.savePekerja(
                                                                pekerja
                                                            )
                                                            sharedPreferencesManager.savePerusahaan(
                                                                perusahaan
                                                            )
                                                            val intent = Intent(
                                                                this@LoginActivity,
                                                                UserActivity::class.java
                                                            )
                                                            val userBundle = Bundle()
                                                            userBundle.putParcelable(
                                                                "user",
                                                                pekerja
                                                            )
                                                            userBundle.putParcelable(
                                                                "perusahaan",
                                                                perusahaan
                                                            )
                                                            intent.putExtra("data", userBundle)
                                                            startActivity(intent)
                                                        }
                                                    }
                                                }
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
                                    Log.e("FetchDataError", "Failed to fetch data")
                                }
                            })
                            setLoading(false)
//                            if (rememberMeChecked) {
//                                sharedPreferencesManager.savePekerja(pekerja)
//                                sharedPreferencesManager.savePerusahaan(perusahaan)
//                            }
//                            val intent = Intent(this, UserActivity::class.java)
//                            val userBundle = Bundle()
//                            userBundle.putParcelable("user", pekerja)
//                            userBundle.putParcelable("perusahaan", perusahaan)
//                            intent.putExtra("data", userBundle)
//                            startActivity(intent)
                        }

                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.d("tanggal", e.toString())
                    }
                },
                { error ->
                    MotionToast.createToast(this@LoginActivity, "Error",
                        "Email atau Password Anda Salah",
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.LONG_DURATION,
                        ResourcesCompat.getFont(this@LoginActivity, R.font.ralewaybold))
                    login.revertAnimation()
                }
            )
            Volley.newRequestQueue(this).add(request)
//          }
//        Log.d("tanggal", "Error2")
        setLoading(false)
    }
    private fun parseDate(dateString: String): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }
    // Function to convert vector drawable to Bitmap
    private fun vectorToBitmap(vectorDrawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }

}