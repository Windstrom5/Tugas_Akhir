package com.windstrom5.tugasakhir.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayout
import com.mindinventory.midrawer.MIDrawerView
import com.saadahmedev.popupdialog.PopupDialog
import com.saadahmedev.popupdialog.listener.StandardDialogActionListener
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.adapter.NewsPagerAdapter
import com.windstrom5.tugasakhir.connection.SharedPreferencesManager
import com.windstrom5.tugasakhir.databinding.ActivityAdminBinding
import com.windstrom5.tugasakhir.feature.News
import com.windstrom5.tugasakhir.feature.Post
import com.windstrom5.tugasakhir.model.Absen
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import de.hdodenhof.circleimageview.CircleImageView
import eo.view.signalstrength.SignalStrengthView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var tv: TextView
    private var bundle: Bundle? = null
    private var perusahaan: Perusahaan? = null
    private lateinit var tvnamaPerusahaan: TextView
    private lateinit var absen: CardView
    private lateinit var lembur: CardView
    private lateinit var dinas: CardView
    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private lateinit var izin: CardView
    private lateinit var cs: Button
    private lateinit var back: ImageView
    private var admin: Admin? = null
    private lateinit var imageView: ImageView
    private lateinit var day: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var location: TextView
    private lateinit var quoteOfTheDay: TextView
    private lateinit var laporan: Button

    //    private lateinit var weatherImage: ImageView
    private var weather: String? = null
    private var temp: String? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var newsAdapter: NewsPagerAdapter
    private var currentLocation: String? = null
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var horizontalScrollView: HorizontalScrollView
    private lateinit var btnPrevious2: ImageButton
    private lateinit var btnNext2: ImageButton
    private lateinit var pingTextView: TextView
    private lateinit var nama: TextView
    private lateinit var signalStrengthView: SignalStrengthView
    private lateinit var circleImageView: CircleImageView
    private lateinit var address: TextView
    private lateinit var drawerLayout: MIDrawerView
    private lateinit var toolbar: Toolbar
    private lateinit var home: Button
    private lateinit var setting: Button
    private lateinit var logout: Button
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var isFetching = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_menu_24)
        swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)

        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        val menuLayout = layoutInflater.inflate(R.layout.menu_layout, null) as RelativeLayout
        signalStrengthView = menuLayout.findViewById(R.id.signal_strength_view)
        laporan = menuLayout.findViewById(R.id.menu_laporan)
        pingTextView = menuLayout.findViewById(R.id.ping_text_view)
        home = menuLayout.findViewById(R.id.menu_home)
        setting = menuLayout.findViewById(R.id.menu_settings)
        logout = menuLayout.findViewById(R.id.menu_logout)
        cs = menuLayout.findViewById(R.id.menu_contact)
        nama = menuLayout.findViewById(R.id.tv_name)
        address = menuLayout.findViewById(R.id.tv_address)
        circleImageView = menuLayout.findViewById(R.id.circleImageView)
        val mainLayout =
            findViewById<RelativeLayout>(R.id.mainLayout)
        mainLayout.addView(menuLayout)
        getBundle()
        swipeRefreshLayout.setOnRefreshListener {
            // Your refresh logic here
            admin?.id?.let { fetchDataFromApi(it,"Admin",null) }
        }
        nama.setText(admin?.nama)
        fetchSignalStrengthContinuously()
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        fetchNewsData()
//        updateViewPagerArrowVisibility()
//        horizontalScrollView = findViewById(R.id.horizontalScrollView)
        imageView = binding.weatherIcon
        day = binding.dayText
        location = binding.location
        quoteOfTheDay = binding.dayText
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermissions()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val namaPerusahaan = perusahaan?.nama
        absen = binding.AbsensiCard
        lembur = binding.LemburCard
        dinas = binding.DinasCard
        izin = binding.IzinCard
        laporan.setOnClickListener {
            val intent = Intent(this, LaporanActivity::class.java)
            startActivityWithExtras(intent)
        }
        setting.setOnClickListener {
            val intent = Intent(this, CompanyActivity::class.java)
            startActivityWithExtras(intent)
        }
        cs.setOnClickListener {
            val intent = Intent(this, CustomerServiceActivity::class.java)
            startActivityWithExtras(intent)
        }
        logout.setOnClickListener {
            PopupDialog.getInstance(this@AdminActivity)
                .standardDialogBuilder()
                .createIOSDialog()
                .setHeading("Logout")
                .setDescription(
                    "Are you sure you want to logout?" +
                            " This action cannot be undone"
                )
                .setPositiveButtonText("Yes") // Set custom text for the positive button
                .setNegativeButtonText("No") // Set custom text for the negative button
                .build(object : StandardDialogActionListener {
                    override fun onPositiveButtonClicked(dialog: Dialog) {
                        val sharedPreferencesManager = SharedPreferencesManager(this@AdminActivity)
                        sharedPreferencesManager.clearUserData()
                        startActivity(Intent(this@AdminActivity, LoginActivity::class.java))
                    }

                    override fun onNegativeButtonClicked(dialog: Dialog) {
                        dialog.dismiss()
                    }
                })
                .let { dialog ->
                    if (!isFinishing) { // Check if the Activity is finishing or destroyed
                        dialog.show()
                    }
                }
        }
        val nama = admin?.nama
        val text = "© $currentYear $namaPerusahaan. \nAll rights reserved."
        tv = binding.courtesyNoticeTextView
        tv.text = text
        absen.setOnTouchListener { _, event -> handleCardTouch(absen, event, "AbsensiActivity") }
        lembur.setOnTouchListener { _, event -> handleCardTouch(lembur, event, "LemburActivity") }
        dinas.setOnTouchListener { _, event -> handleCardTouch(dinas, event, "DinasActivity") }
        izin.setOnTouchListener { _, event -> handleCardTouch(izin, event, "IzinActivity") }
    }

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
                    val batasAktifUtilDate =
                        SimpleDateFormat("yyyy-MM-dd").parse(perusahaanJson.getString("batas_aktif"))
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
                        secret_key = perusahaanJson.getString("secret_key")
                    )

                    val user = response.getJSONObject("user")
                    val sharedPreferencesManager = SharedPreferencesManager(this@AdminActivity)
                    sharedPreferencesManager.clearUserData()
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
                        nama.setText(user.getString("nama"))
                        val url =
                            "http://192.168.1.6:8000/api/Admin/decryptProfile/${admin?.id}" // Replace with your actual URL

                        val imageRequest = ImageRequest(
                            url,
                            { response ->
                                // Set the Bitmap to an ImageView or handle it as needed
                                circleImageView.setImageBitmap(response)
                            },
                            0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                            { error ->
                                error.printStackTrace()
                                Toast.makeText(this, "Failed to fetch profile image", Toast.LENGTH_SHORT).show()
                            }
                        )

                        val requestQueue = Volley.newRequestQueue(this)
                        requestQueue.add(imageRequest)
                        Log.d("VolleyError", admin.toString())
                        sharedPreferencesManager.savePerusahaan(perusahaan)
                        sharedPreferencesManager.saveAdmin(admin)
                    } else {
                        if (response.has("presensi")) {
                            val presensi = response.getJSONObject("perusahaan")
                            val jamMasukpresensi = perusahaanJson.getString("jam_masuk")
                            val jamKeluarpresensi = perusahaanJson.getString("jam_keluar")

                            // Convert time strings to java.sql.Time
                            val jamMasuk = convertStringToTime(jamMasukpresensi)
                            val jamKeluar = convertStringToTime(jamKeluarpresensi)
                            val absen = Absen(
                                presensi.getInt("id"),
                                presensi.getInt("id_pekerja"),
                                presensi.getInt("id_perusahaan"),
                                parseDate(user.getString("tanggal")),
                                presensi.getString("jam_masuk"), // Change to your preferred time representation (e.g., String)
                                presensi.getString("jam_keluar"),// Change to your preferred time representation (e.g., String)
                                presensi.getDouble("latitude"),
                                presensi.getDouble("longitude")
                            )
                            sharedPreferencesManager.savePresensi(absen)
                        }
                        val pekerja = Pekerja(
                            user.getInt("id"),
                            user.getInt("id_perusahaan"),
                            user.getString("email"),
                            user.getString("password"),
                            user.getString("nama"),
                            parseDate(user.getString("tanggal_lahir")),
                            user.getString("profile")
                        )

                        sharedPreferencesManager.savePerusahaan(perusahaan)
                        sharedPreferencesManager.savePekerja(pekerja)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("VolleyError", "Error parsing response: ${e.message}")
                    swipeRefreshLayout.isRefreshing = false
                }
            },
            { error: VolleyError ->
                Log.e("VolleyError", "Error: ${error.message}")
            }
        )

        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(jsonObjectRequest)
        swipeRefreshLayout.isRefreshing = false
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

    private fun fetchSignalStrengthContinuously() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isFetching) {
                try {
                    Log.d("Pinged", "Attempting to ping")
                    val url = URL("http://192.168.1.6:8000") // Replace with your actual endpoint
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.connect()
                    Log.d("Pinged", "Connected")

                    val responseCode = connection.responseCode
                    Log.d("Pinged", "Response Code: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = StringBuilder()
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))

                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        val pingMs = response.toString().toIntOrNull() ?: 0
                        Log.d("Pinged", "Ping Value: $pingMs")
                        val signalStrength = when {
                            pingMs < 50 -> 100 // Very strong signal
                            pingMs < 100 -> 80  // Strong signal
                            pingMs < 150 -> 60  // Good signal
                            pingMs < 200 -> 40  // Fair signal
                            else -> 20           // Weak signal
                        }
                        withContext(Dispatchers.Main) {
                            signalStrengthView.signalLevel = signalStrength
                            pingTextView.text = " $pingMs ms"
                            signalStrengthView.color =
                                ContextCompat.getColor(this@AdminActivity, R.color.colorAccent)
                        }
                    } else {
                        Log.e("Pinged", "Error: Received response code $responseCode")
                    }

                    connection.disconnect()
                    Log.d("Pinged", "Disconnected")
                } catch (e: IOException) {
                    Log.e("Pinged", "IOException: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        // Handle UI updates or notifications for offline state
                        signalStrengthView.signalLevel =
                            0 // Set signal strength to 0 or indicate offline
                        pingTextView.text = "Offline" // Update UI to indicate offline status
                        signalStrengthView.color = Color.RED
                    }
                } catch (e: Exception) {
                    Log.e("Pinged", "Exception: ${e.message}", e)
                } finally {
                    delay(5000) // Delay before making the next ping attempt
                }
            }
        }
    }


    private fun fetchNewsData() {
        val url = "https://api-berita-indonesia.vercel.app/cnn/terbaru"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val news = parseNewsResponse(response)
                setupViewPager(news)
            },
            { error ->
                error.printStackTrace()
            })

        Volley.newRequestQueue(this).add(request)
    }

    private fun parseNewsResponse(response: JSONObject): List<News> {
        val newsList = mutableListOf<News>()
        val data = response.optJSONObject("data")
        val posts = data.optJSONArray("posts")

        for (i in 0 until posts.length()) {
            val post = posts.optJSONObject(i)
            val news = News(
                data.optString("link"),
                data.optString("description"),
                data.optString("title"),
                data.optString("image"),
                parsePosts(post)
            )
            newsList.add(news)
        }
        return newsList.take(5) // Take only the first 5 news items
    }

    private fun parsePosts(postJson: JSONObject): List<Post> {
        val posts = mutableListOf<Post>()
        posts.add(
            Post(
                postJson.optString("link"),
                postJson.optString("title"),
                postJson.optString("pubDate"),
                postJson.optString("description"),
                postJson.optString("thumbnail")
            )
        )
        return posts
    }

    private fun setupViewPager(newsList: List<News>) {
        val adapter = NewsPagerAdapter(newsList)
        viewPager.adapter = adapter

        // Update arrow visibility
        updateViewPagerArrowVisibility()

        // Add a page change listener to update the visibility of the arrows
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateViewPagerArrowVisibility()
            }
        })

        // Set up the arrow click listeners
        btnPrevious.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem > 0) {
                viewPager.currentItem = currentItem - 1
            }
        }

        btnNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < viewPager.adapter!!.itemCount - 1) {
                viewPager.currentItem = currentItem + 1
            }
        }
    }

    private fun updateViewPagerArrowVisibility() {
        btnPrevious.visibility = if (viewPager.currentItem == 0) View.GONE else View.VISIBLE
        btnNext.visibility =
            if (viewPager.currentItem == viewPager.adapter!!.itemCount - 1) View.GONE else View.VISIBLE
    }

    private fun requestLocationPermissions() {
        val hasFineLocationPermission =
            checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        val hasCoarseLocationPermission =
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

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
            getLastLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permissions are required to use this feature.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val latitude = it.latitude
                    val longitude = it.longitude
                    Log.d("location", latitude.toString() + "," + longitude.toString())
                    getDesaKelurahanFromLocation(latitude, longitude)
                }
            }
    }

    private fun getDesaKelurahanFromLocation(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val desaKelurahan = address.subLocality ?: "Unknown"
                val kecamatan = address.locality ?: "Unknown"
                val kabupatenKota = address.subAdminArea ?: "Unknown"
                val province = address.adminArea ?: "Unknown"

                Log.d(
                    "Location",
                    "Desa/Kelurahan: $desaKelurahan, Kecamatan: $kecamatan, Kabupaten/Kota: $kabupatenKota, Province: $province"
                )
                Toast.makeText(this, "Desa/Kelurahan: $desaKelurahan", Toast.LENGTH_SHORT).show()
                fetchWeatherData(desaKelurahan)
                val wilayahJsonString =
                    resources.openRawResource(R.raw.wilayah).bufferedReader().use { it.readText() }
                val wilayahArray = JSONArray(wilayahJsonString)
                return findMatchingLocation(wilayahArray, kecamatan, kabupatenKota, province)
            } else {
                Toast.makeText(this, "No address found for location.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to get desa/kelurahan from location.", Toast.LENGTH_SHORT)
                .show()
        }
        return null
    }

    private fun findMatchingLocation(
        wilayahArray: JSONArray,
        kecamatan: String,
        kabupatenKota: String,
        province: String
    ): String? {
        // Threshold for similarity score
        val threshold = 0.8// You can adjust this threshold as needed

        // Preprocess kabupatenKota string to remove prefixes
        val preprocessedKabupatenKota = kabupatenKota.replace("Kabupaten ", "").replace("Kota ", "")
        for (i in 0 until wilayahArray.length()) {
            val wilayahObject: JSONObject = wilayahArray.getJSONObject(i)
            val propinsi = wilayahObject.getString("propinsi")
            // Calculate similarity scores
            val kota = wilayahObject.getString("kota")
            val preprocessedKota = kota.replace("Kab. ", "").replace("Kota ", "")
            val kecamatanMatchScore =
                similarityScore(kecamatan, wilayahObject.getString("kecamatan"))
            val kabupatenKotaMatchScore =
                similarityScore(preprocessedKabupatenKota, preprocessedKota)
            val provinceMatchScore = similarityScore(province, propinsi)

            // Check if any score exceeds the threshold
            if (kecamatanMatchScore >= threshold || kabupatenKotaMatchScore >= threshold || provinceMatchScore >= threshold) {
                val id = wilayahObject.getString("id")
                val kota3 = wilayahObject.getString("kota")
                val preprocessedKota = kota3.replace("Kabupaten ", "").replace("Kota ", "")
                currentLocation = preprocessedKota + ", " + province
                address.setText(currentLocation)
                Log.d("Location2", "Partial match found! ID: $id")
                return id
            }
        }

        // If no match found, return null
        return null
    }

    private fun similarityScore(str1: String, str2: String): Double {
        val set1 = str1.toSet()
        val set2 = str2.toSet()
        val intersectionSize = set1.intersect(set2).size
        val unionSize = set1.union(set2).size
        return intersectionSize.toDouble() / unionSize.toDouble()
    }

    private fun fetchWeatherData(location: String) {
        val url = "https://wttr.in/${location}?format=j1"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    // Parse the response string as JSON
                    val jsonResponse = JSONObject(response)
                    processWeatherData(jsonResponse)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to parse weather data.", Toast.LENGTH_SHORT).show()
                    fetchHolidayData()
                }
            },
            { error ->
                // Ignore the 404 error if we still receive a response
                val networkResponse = error.networkResponse
                if (networkResponse?.statusCode == 404) {
                    val responseString = String(networkResponse.data)
                    try {
                        // Parse the response string as JSON
                        val jsonResponse = JSONObject(responseString)
                        processWeatherData(jsonResponse)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        fetchHolidayData()
                        Toast.makeText(this, "Failed to parse weather data.", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    error.printStackTrace()
                    Toast.makeText(this, "Unable to fetch weather data.", Toast.LENGTH_SHORT).show()
                    fetchHolidayData()
                }
            }
        )
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun updateWeatherIcon(weatherDesc: String) {
        when {
            weatherDesc.contains("Clear", ignoreCase = true) -> {
                weather = "Clear Skies"
                Glide.with(this)
                    .load(R.drawable.sunny)
                    .into(imageView)
            }

            weatherDesc.contains("Partly cloudy", ignoreCase = true) -> {
                weather = "Partly Cloudy"
                Glide.with(this)
                    .load(R.drawable.sunnycloudy)
                    .into(imageView)
            }

            weatherDesc.contains("Cloudy", ignoreCase = true) -> {
                weather = "Cloudy"
                Glide.with(this)
                    .load(R.drawable.cloudy)
                    .into(imageView)
            }

            weatherDesc.contains("Overcast", ignoreCase = true) -> {
                weather = "Overcast"
                Glide.with(this)
                    .load(R.drawable.cloudy)
                    .into(imageView)
            }

            weatherDesc.contains("Rain", ignoreCase = true) -> {
                weather = "Rain"
                Glide.with(this)
                    .load(R.drawable.rain)
                    .into(imageView)
            }

            weatherDesc.contains("Thunderstorm", ignoreCase = true) -> {
                weather = "Thunderstorm"
                Glide.with(this)
                    .load(R.drawable.storm)
                    .into(imageView)
            }

            weatherDesc.contains("Sunny", ignoreCase = true) -> {
                weather = "Sunny"
                Glide.with(this)
                    .load(R.drawable.sunny)
                    .into(imageView)
            }
//            weatherDesc.contains("Haze", ignoreCase = true) -> {
//                weather = "Haze"
//                Glide.with(this)
//                    .load(R.drawable.haze)
//                    .into(imageView)
//            }
//            weatherDesc.contains("Snow", ignoreCase = true) -> {
//                weather = "Snow"
//                Glide.with(this)
//                    .load(R.drawable.snow)
//                    .into(imageView)
//            }
//            weatherDesc.contains("Fog", ignoreCase = true) -> {
//                weather = "Fog"
//                Glide.with(this)
//                    .load(R.drawable.fog)
//                    .into(imageView)
//            }
            else -> {
                weather = "Unknown"
                Glide.with(this)
                    .load(R.drawable.baseline_emoji_people_24)
                    .into(imageView)
            }
        }
        Log.d("weather", weather!!)
    }


    private fun processWeatherData(response: JSONObject) {
        val currentCondition = response.getJSONArray("current_condition").getJSONObject(0)
        val weatherDesc =
            currentCondition.getJSONArray("weatherDesc").getJSONObject(0).getString("value")
        val tempC = currentCondition.getString("temp_C")
        fetchHolidayData()
        val humidity = currentCondition.getString("humidity")
        val pressure = currentCondition.getString("pressure")
        val windSpeedKmph = currentCondition.getString("windspeedKmph")
        val observationTime = currentCondition.getString("observation_time")

        // Log and display weather details
        location.setText("Today Temperature Is " + tempC + "°C At " + currentLocation)
        // Update UI based on weather description
        updateWeatherIcon(weatherDesc)
    }


    private fun fetchHolidayData() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Construct the URL with the current year
        val url = "https://dayoffapi.vercel.app/api?year=$currentYear"

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                checkTodayHoliday(response)
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "Unable to fetch holiday data.", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(jsonArrayRequest)
    }

    private fun checkTodayHoliday(holidays: JSONArray) {
        val today =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        var isHoliday = false

        for (i in 0 until holidays.length()) {
            val holiday = holidays.getJSONObject(i)
            if (holiday.getString("tanggal") == today) {
                day.text = holiday.getString("keterangan")
                isHoliday = true
                break
            }
        }
        val quotes: Map<String, String> // Declare the quotes variable outside the if-else block
        weather?.let { Log.d("weather", it) }
        if (!isHoliday) {
            val dayName =
                SimpleDateFormat("EEEE", Locale.getDefault()).format(Calendar.getInstance().time)
            val view : FrameLayout = binding.view
            view.visibility = View.VISIBLE
            if(weather == null ){
                quotes = mapOf(
                    "Monday" to "Happy Monday ${admin?.nama}! Start your week with a smile!",
                    "Tuesday" to "It's a Tuesday ${admin?.nama}! Keep going strong!",
                    "Wednesday" to "Wonderful Wednesday ${admin?.nama}! You're halfway there!",
                    "Thursday" to "Thrilling Thursday ${admin?.nama}! Almost the weekend!",
                    "Friday" to "Fantastic Friday ${admin?.nama}! Enjoy the day!",
                    "Saturday" to "Superb Saturday ${admin?.nama}! Have a great weekend!",
                    "Sunday" to "Relaxing Sunday ${admin?.nama}! Rest and recharge!"
                )
                location.setText(currentLocation)
                Glide.with(this)
                    .load(R.drawable.baseline_emoji_people_24)
                    .into(imageView)
            }else{
                quotes = mapOf(
                    "Monday" to "$weather Monday ${admin?.nama}! Start your week with a smile!",
                    "Tuesday" to "It's a $weather Tuesday ${admin?.nama}! Keep going strong!",
                    "Wednesday" to "Wonderful $weather Wednesday ${admin?.nama}! You're halfway there!",
                    "Thursday" to "Thrilling $weather Thursday ${admin?.nama}! Almost the weekend!",
                    "Friday" to "Fantastic $weather Friday ${admin?.nama}! Enjoy the day!",
                    "Saturday" to "Superb $weather Saturday ${admin?.nama}! Have a great weekend!",
                    "Sunday" to "Relaxing $weather Sunday ${admin?.nama}! Rest and recharge!"
                )
            }
            day.text = dayName
            quoteOfTheDay.text = quotes[dayName] ?: "Have a great day!"
        }
    }

    override fun onBackPressed() {
        PopupDialog.getInstance(this@AdminActivity)
            .standardDialogBuilder()
            .createIOSDialog()
            .setHeading("Logout")
            .setDescription(
                "Are you sure you want to logout?" +
                        " This action cannot be undone"
            )
            .setPositiveButtonText("Yes") // Set custom text for the positive button
            .setNegativeButtonText("No") // Set custom text for the negative button
            .build(object : StandardDialogActionListener {
                override fun onPositiveButtonClicked(dialog: Dialog) {
                    val sharedPreferencesManager = SharedPreferencesManager(this@AdminActivity)
                    sharedPreferencesManager.clearUserData()
                    startActivity(Intent(this@AdminActivity, LoginActivity::class.java))
                }

                override fun onNegativeButtonClicked(dialog: Dialog) {
                    dialog.dismiss()
                }
            })
            .let { dialog ->
                if (!isFinishing) { // Check if the Activity is finishing or destroyed
                    dialog.show()
                }
            }
    }

    private fun handleCardTouch(
        cardView: CardView,
        event: MotionEvent,
        activityName: String
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.green))
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.whiteTextColor
                    )
                )
                when (activityName) {
                    "AbsensiActivity" -> {
                        val intent = Intent(this, AbsensiActivity::class.java)
                        startActivityWithExtras(intent)
                    }

                    "LemburActivity" -> {
                        val intent = Intent(this, LemburActivity::class.java)
                        startActivityWithExtras(intent)
                    }

                    "DinasActivity" -> {
                        val intent = Intent(this, DinasActivity::class.java)
                        startActivityWithExtras(intent)
                    }

                    "IzinActivity" -> {
                        val intent = Intent(this, IzinActivity::class.java)
                        startActivityWithExtras(intent)
                    }

                    "CompanyActivity" -> {
                        val intent = Intent(this, CompanyActivity::class.java)
                        startActivityWithExtras(intent)
                    }

                    "CsActivity" -> {
                        val intent = Intent(this, CustomerServiceActivity::class.java)
                        startActivityWithExtras(intent)
                    }
                }
            }
        }
        return true
    }

    private fun startActivityWithExtras(intent: Intent) {
        val userBundle = Bundle()
        userBundle.putParcelable("user", admin)
        userBundle.putParcelable("perusahaan", perusahaan)
        userBundle.putString("role", "Admin")
        intent.putExtra("data", userBundle)
        startActivity(intent)
    }

    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                perusahaan = it.getParcelable("perusahaan")
                admin = it.getParcelable("user")
            }
            val url =
                "http://192.168.1.6:8000/api/Admin/decryptProfile/${admin?.id}" // Replace with your actual URL

            val imageRequest = ImageRequest(
                url,
                { response ->
                    // Set the Bitmap to an ImageView or handle it as needed
                    circleImageView.setImageBitmap(response)
                },
                0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                { error ->
                    error.printStackTrace()
                    Toast.makeText(this, "Failed to fetch profile image", Toast.LENGTH_SHORT).show()
                }
            )

            val requestQueue = Volley.newRequestQueue(this)
            requestQueue.add(imageRequest)
        } else {
            Log.d("Error", "Bundle Not Found")
        }
    }
}
