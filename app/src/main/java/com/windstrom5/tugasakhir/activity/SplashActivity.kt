package com.windstrom5.tugasakhir.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.connection.SharedPreferencesManager
import com.windstrom5.tugasakhir.model.Absen
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.login_session
import okhttp3.ResponseBody
import org.apache.commons.lang3.time.DateUtils.parseDate
import org.json.JSONException
import org.json.JSONObject
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.sql.Time
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SplashActivity : AppCompatActivity() {
    private var perusahaanList: List<Perusahaan> = emptyList()
    private val splashTimeOut: Long = 2000 // 2 seconds
    private lateinit var logoImageView: ImageView
    // Define LOCATION_PERMISSION_REQUEST_CODE here
    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private var savedAdmin: Admin? = null
    private var savedPekerja: Pekerja? = null
    private var savedPerusahaan: Perusahaan? = null
    private var presensi: Absen? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        logoImageView = findViewById(R.id.logoImageView)
        Log.d("VolleyError","test")
        Glide.with(this)
            .load(R.drawable.logo)
            .into(logoImageView)
        requestLocationPermissions()
//        val sharedPreferencesManager = SharedPreferencesManager(this)
//        val savedSession = sharedPreferencesManager.getSession()
//        if(savedSession!= null && checkSession(savedSession) == true){
//            redirectToActivity(savedSession)
//        }else{
//            Handler(Looper.getMainLooper()).postDelayed({
//                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
//                startActivity(intent)
//                finish()
//            }, splashTimeOut)
//        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun fetchDataFromApi(id: Int, jenis: String, id_presensi: Int?) {
        val url = if (id_presensi == null) {
            "http://192.168.1.6:8000/api/getData?id=$id&jenis=$jenis"
        } else {
            "http://192.168.1.6:8000/api/getData?id=$id&jenis=$jenis&id_presensi=$id_presensi"
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val role = response.getString("Role")
                    val perusahaanJson = response.getJSONObject("perusahaan")

                    // Extract time strings
                    val jamMasukStr = perusahaanJson.getString("jam_masuk")
                    val jamKeluarStr = perusahaanJson.getString("jam_keluar")

                    // Convert time strings to java.sql.Time
                    val jamMasuk = convertStringToTime(jamMasukStr)
                    val jamKeluar = convertStringToTime(jamKeluarStr)
                    val batasAktifUtilDate = SimpleDateFormat("yyyy-MM-dd").parse(perusahaanJson.getString("batas_aktif"))
                    val batasAktifSqlDate = java.sql.Date(batasAktifUtilDate.time)
                    // Construct the Perusahaan object manually
                    val perusahaan = Perusahaan(
                        id = perusahaanJson.getInt("id"),
                        nama = perusahaanJson.getString("nama"),
                        latitude = perusahaanJson.getDouble("latitude"),
                        longitude = perusahaanJson.getDouble("longitude"),
                        jam_masuk = jamMasuk,
                        jam_keluar = jamKeluar,
                        batasAktif = batasAktifSqlDate,
                        logo = if (perusahaanJson.has("logo")) perusahaanJson.getString("logo") else null,
                        secret_key = perusahaanJson.getString("secret_key"),
                        holiday = perusahaanJson.getString("holiday")
                    )

                    val user = response.getJSONObject("user")
                    val sharedPreferencesManager = SharedPreferencesManager(this@SplashActivity)
                    Log.d("VolleyError",jenis.toString())
                    if (jenis == "Admin") {

                        val admin = Admin(
                            user.getInt("id"),
                            user.getInt("id_perusahaan"),
                            user.getString("email"),
                            user.getString("password"),
                            user.getString("nama"),
                            parseDate(user.getString("tanggal_lahir")),
                            user.getString("profile")
                        )
                        Log.d("VolleyError",admin.toString())
                        sharedPreferencesManager.clearUserData()
                        sharedPreferencesManager.savePerusahaan(perusahaan)
                        sharedPreferencesManager.saveAdmin(admin)
                    } else {
                        if (response.has("presensi")) {
                            showToast("Masih Masuk Presensi Tidak Bisa Login")
                        }else{
                            val pekerja = Pekerja(
                                user.getInt("id"),
                                user.getInt("id_perusahaan"),
                                user.getString("email"),
                                user.getString("password"),
                                user.getString("nama"),
                                parseDate(user.getString("tanggal_lahir")),
                                user.getString("profile")
                            )
                            sharedPreferencesManager.clearUserData()
                            sharedPreferencesManager.savePerusahaan(perusahaan)
                            sharedPreferencesManager.savePekerja(pekerja)
                        }
                    }


                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("VolleyError", "Error parsing response: ${e.message}")
                }
            },
            { error: VolleyError ->
                Log.e("VolleyError", "Error: ${error.message}")
            }
        )

        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(jsonObjectRequest)
    }

    private fun parseDate(dateString: String): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }

    @SuppressLint("SimpleDateFormat")
    fun convertStringToTime(timeStr: String): Time {
        val sdf = SimpleDateFormat("HH:mm:ss")
        val date: Date = sdf.parse(timeStr)
        return Time(date.time)
    }


    private fun requestLocationPermissions() {
        // Check whether your app already has the permissions.
        val hasFineLocationPermission =
            checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        val hasCoarseLocationPermission =
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        // If permissions are not granted, request them
        if (!hasFineLocationPermission || !hasCoarseLocationPermission) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions already granted, continue with the splash
            continueWithSplash()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // Permission denied, check if "Don't ask again" is selected
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                    shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                ) {
                    // User denied permission but didn't select "Don't ask again"
                    // Show a toast or handle it accordingly
                    showToast("Location permission is required.")
                    finish()
                } else {
                    // User denied permission and selected "Don't ask again"
                    // Show a toast and direct the user to app settings
                    showToastWithDelay("Please enable Location permission.")
                }
            } else {
                // Permission granted, continue with the splash
                continueWithSplash()
            }
        }
    }
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun showToast(message: String) {
        MotionToast.createToast(
            this,
            "Error",
            message,
            MotionToastStyle.ERROR,
            MotionToast.GRAVITY_BOTTOM,
            MotionToast.LONG_DURATION,
            ResourcesCompat.getFont(this,R.font.ralewaybold)
        )
    }
    private fun showToastWithDelay(message: String) {
        showToast(message)
        Handler(Looper.getMainLooper()).postDelayed({
            openAppSettings()
        }, MotionToast.LONG_DURATION)
    }
    private fun continueWithSplash() {
        val sharedPreferencesManager = SharedPreferencesManager(this)
        savedAdmin = sharedPreferencesManager.getAdmin()
        savedPekerja = sharedPreferencesManager.getPekerja()
        savedPerusahaan= sharedPreferencesManager.getPerusahaan()
        presensi= sharedPreferencesManager.getPresensi()
        if (savedAdmin != null || savedPekerja != null) {
            redirectToActivity(savedPerusahaan,savedAdmin,savedPekerja,presensi)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }, splashTimeOut)
        }
    }
    private fun redirectToActivity(perusahaan: Perusahaan?, admin: Admin?, pekerja: Pekerja?, presensi: Absen?) {
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val logo = perusahaan?.logo
        if (logo == "null") {
            Glide.with(this)
                .load(R.drawable.logo)
                .into(logoImageView)
        } else {
            val url =
                "http://192.168.1.6:8000/api/Perusahaan/decryptLogo/${perusahaan?.id}" // Replace with your actual URL

            val imageRequest = ImageRequest(
                url,
                { response ->
                    // Set the Bitmap to an ImageView or handle it as needed
                    logoImageView.setImageBitmap(response)
                },
                0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                { error ->
                    error.printStackTrace()
                    Toast.makeText(this, "Failed to fetch profile image", Toast.LENGTH_SHORT).show()
                }
            )

            val requestQueue = Volley.newRequestQueue(this)
            requestQueue.add(imageRequest)
        }
        if (admin != null) {
            admin.id?.let { fetchDataFromApi(it, "Admin", presensi?.id) }
        }else if(pekerja !=null){
            pekerja.id?.let { fetchDataFromApi(it,"Pekerja",presensi?.id) }
        }
        Log.d("currecnt",savedPerusahaan.toString())
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = if (admin != null) {
                Intent(this@SplashActivity, AdminActivity::class.java).apply {
                    putExtra("data", Bundle().apply {
                        putParcelable("user", savedAdmin)
                        putParcelable("perusahaan", savedPerusahaan)
                    })
                }
            } else {
                Intent(this@SplashActivity, UserActivity::class.java).apply {
                    putExtra("data", Bundle().apply {
                        putParcelable("user", savedPekerja)
                        putParcelable("perusahaan", savedPerusahaan)
                        if (presensi != null) {
                            putParcelable("presensi", presensi)
                        }
                    })
                }
            }
            startActivity(intent)
            if (admin != null) {
                finish()
            } else {
                Animatoo.animateCard(this@SplashActivity)
                finish()
            }
        }, splashTimeOut)
    }


    private fun checkSession(session: login_session):Boolean{
        val currentTime = System.currentTimeMillis()
        val sessionCreateTime = session.create_at.time
        val sessionDurationMillis = 8 * 60 * 60 * 1000 // 8 hours in milliseconds
        return currentTime - sessionCreateTime < sessionDurationMillis
    }

}