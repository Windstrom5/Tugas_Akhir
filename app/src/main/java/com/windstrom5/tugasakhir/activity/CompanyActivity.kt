package com.windstrom5.tugasakhir.activity

import android.annotation.SuppressLint
import com.windstrom5.tugasakhir.connection.SharedPreferencesManager
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.OnMenuItemClickListener
import com.skydoves.powermenu.PowerMenu
import com.skydoves.powermenu.PowerMenuItem
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.adapter.ListAnggotaAdapter
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.connection.ReverseGeocoder
import com.windstrom5.tugasakhir.databinding.ActivityCompanyBinding
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import kotlinx.coroutines.Job
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CompanyActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCompanyBinding
    private var admin : Admin? = null
    private var pekerja : Pekerja? = null
    private var bundle: Bundle? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var perusahaan : Perusahaan? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ListAnggotaAdapter
    private lateinit var role:String
    private lateinit var requestQueue: RequestQueue
    private lateinit var addPekerja: Button
    private lateinit var setting:ImageView
    private var job: Job? = null
    private var fetchRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private val pollingInterval = 2000L // Polling interval in milliseconds
    private lateinit var countpekerja : TextView
    private lateinit var countadmin : TextView
    private lateinit var address : TextView
    private lateinit var namaTv : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        addPekerja = binding.addPekerja
        setting = binding.setting
        address = binding.tvAddress
        namaTv = binding.tvName
        getBundle()
        recyclerView = findViewById(R.id.recyclerViewPekerja)
        adapter =
            perusahaan?.let { ListAnggotaAdapter(mutableListOf(), mutableListOf(), it,role,this@CompanyActivity) }!!// Initialize with empty lists
        recyclerView.adapter = adapter
        val layoutManager = GridLayoutManager(this, 2)
        recyclerView.layoutManager = layoutManager
        requestQueue = Volley.newRequestQueue(this)
        countpekerja = binding.countpekerja
        countadmin = binding.countadmin
        perusahaan?.let { fetchDataFromApi(it.nama) }
        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            perusahaan?.let { fetchDataFromApi(it.nama) }
        }
        addPekerja.setOnClickListener{
            val intent = Intent(this, RegisterPekerjaActivity::class.java)
            val userBundle = Bundle()
            userBundle.putParcelable("user", admin)
            userBundle.putParcelable("perusahaan", perusahaan)
            userBundle.putString("role", "Admin")
            intent.putExtra("data", userBundle)
            startActivity(intent)
        }
        setting.setOnClickListener{
            if(admin!= null){
                val powerMenu = PowerMenu.Builder(this)
                    .addItem(PowerMenuItem("Edit profile", false))
                    .addItem(PowerMenuItem("Edit Company", false))
                    .addItem(PowerMenuItem("Add Worker", false))
                    .addItem(PowerMenuItem("Delete Company", false))
                    .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
                    .setMenuRadius(10f)
                    .setMenuShadow(10f)
                    .setTextColorResource(R.color.blackTextColor)
                    .setTextSize(12)
                    .setSelectedTextColorResource(R.color.white)
                    .setMenuColor(Color.WHITE)
                    .setOnMenuItemClickListener(object : OnMenuItemClickListener<PowerMenuItem> {
                        override fun onItemClick(position: Int, item: PowerMenuItem) {
                            when (position) {
                                0 -> {
                                    val intent = Intent(this@CompanyActivity, EditUserActivity::class.java)
                                    val userBundle = Bundle()
                                    userBundle.putParcelable("user", admin)
                                    userBundle.putParcelable("perusahaan", perusahaan)
                                    userBundle.putString("role", "Admin")
                                    intent.putExtra("data", userBundle)
                                    startActivity(intent)
                                }
                                1 -> {
                                    val intent = Intent(this@CompanyActivity, EditCompany::class.java)
                                    val userBundle = Bundle()
                                    userBundle.putParcelable("user", admin)
                                    userBundle.putParcelable("perusahaan", perusahaan)
                                    userBundle.putString("role", "Admin")
                                    intent.putExtra("data", userBundle)
                                    startActivity(intent)
                                }
                                2 -> {
                                    // Handle add worker option
                                    val intent = Intent(this@CompanyActivity, RegisterPekerjaActivity::class.java)
                                    val userBundle = Bundle()
                                    userBundle.putParcelable("user", admin)
                                    userBundle.putParcelable("perusahaan", perusahaan)
                                    userBundle.putString("role", "Admin")
                                    intent.putExtra("data", userBundle)
                                    startActivity(intent)
                                }
                                3->{
                                    showDialogWithIcon(perusahaan!!.nama)
                                }
                            }
                        }
                    })
                    .build()

                powerMenu.showAsDropDown(it)
            }else {
                val powerMenu = PowerMenu.Builder(this)
                    .addItem(PowerMenuItem("Edit profile", false))
                    .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
                    .setMenuRadius(10f)
                    .setMenuShadow(10f)
                    .setTextColorResource(R.color.blackTextColor)
                    .setTextSize(12)
                    .setSelectedTextColorResource(R.color.white)
                    .setMenuColor(Color.WHITE)
                    .setOnMenuItemClickListener(object : OnMenuItemClickListener<PowerMenuItem> {
                        override fun onItemClick(position: Int, item: PowerMenuItem) {
                            when (position) {
                                0 -> {
                                    val intent = Intent(this@CompanyActivity, EditUserActivity::class.java)
                                    val userBundle = Bundle()
                                    userBundle.putParcelable("user", pekerja)
                                    userBundle.putParcelable("perusahaan", perusahaan)
                                    userBundle.putString("role", "Pekerja")
                                    intent.putExtra("data", userBundle)
                                    startActivity(intent)
                                }
                            }
                        }
                    })
                    .build()

                powerMenu.showAsDropDown(it)
            }
        }
    }
    private fun deleteCompany(idPerusahaan: Int, callback: (Boolean) -> Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.5:8000/api/") // Replace with your base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)
        val call = service.deleteCompany(idPerusahaan)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback.invoke(true)
                } else {
                    callback.invoke(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                callback.invoke(false)
            }
        })
    }
    private fun showDialogWithIcon(namaPerusahaan: String) {
        val loadingDialog = ProgressDialog.show(this@CompanyActivity, "Loading", "Please wait...", true, false)
        AlertDialog.Builder(this@CompanyActivity)
            .setCancelable(true)
            .setMessage("Are You Sure Want To Delete Your $namaPerusahaan Company?")
            .setTitle("Confirmation")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                loadingDialog.show()
                perusahaan?.id?.let {
                    deleteCompany(it) { success ->
                        loadingDialog.dismiss() // Dismiss the loading dialog
                        if (success) {
                            showSuccessDialog()
                        } else {
                            showErrorDialog()
                        }
                    }
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun showSuccessDialog() {
        AlertDialog.Builder(this@CompanyActivity)
            .setMessage("Company deleted successfully")
            .setTitle("Success")
            .setPositiveButton("OK") { dialog, _ ->
                val sharedPreferencesManager = SharedPreferencesManager(this)
                sharedPreferencesManager.clearUserData()
                startActivity(Intent(this@CompanyActivity, LoginActivity::class.java))
                finish() // Finish the current activity
            }
            .show()
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(this@CompanyActivity)
            .setMessage("Failed to delete company")
            .setTitle("Error")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun fetchDataFromApi(namaPerusahaan: String) {
        val url = "http://192.168.1.5:8000/api/"
        Log.d("FetchDataError", "Nama: ${namaPerusahaan}")
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
//        fetchRunnable = object : Runnable {
//            override fun run() {fdid
                val call = apiService.getDataPekerja(namaPerusahaan)
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            response.body()?.let { responseBody ->
                                try {
                                    val responseData = JSONObject(responseBody.string())
                                    val adminArray = responseData.getJSONArray("admin")
                                    val perusahaanObject = responseData.optJSONObject("perusahaan")

                                    if (perusahaanObject != null) {
                                        val id = perusahaanObject.optInt("id")
                                        val nama = perusahaanObject.getString("nama")
                                        namaTv.setText(nama)
                                        val latitude = perusahaanObject.getDouble("latitude")
                                        val longitude = perusahaanObject.getDouble("longitude")
                                        val addressInfo = ReverseGeocoder.getAddressFromLocation(this@CompanyActivity, GeoPoint(latitude, longitude))
                                        if (addressInfo != null) {
                                            val addressText = "${addressInfo.province}\n${addressInfo.country}"
                                            address.text = addressText
                                        } else {
                                            address.text = "Address information not available"
                                        }
                                        Log.d("FetchDataError", "Nama: ${nama}")
                                        val jamMasukString = perusahaanObject.getString("jam_masuk")
                                        val jamKeluarString = perusahaanObject.getString("jam_keluar")
                                        val jamMasuk = Time.valueOf(jamMasukString)
                                        val jamKeluar = Time.valueOf(jamKeluarString)
                                        val batasAktifString = perusahaanObject.getString("batas_aktif")
                                        val batasAktif = java.sql.Date.valueOf(batasAktifString) // Use java.sql.Date for consistency
                                        val logo = perusahaanObject.getString("logo")
                                        val secretKey = perusahaanObject.getString("secret_key")
                                        val holiday = perusahaanObject.getString("holiday")
                                        Perusahaan(id, nama, latitude, longitude, jamMasuk, jamKeluar, batasAktif, logo, secretKey,holiday)
                                    } else {
                                        null
                                    }
                                    val adminList = parseAdminList(adminArray)
                                    val jumlahPekerja = responseData.optInt("jumlahpekerja", 0)
                                    val jumlahAdmin = responseData.optInt("jumlahadmin", 0)
                                    countpekerja.setText(jumlahPekerja.toString())
                                    countadmin.setText(jumlahAdmin.toString())
                                    val pekerjaArray = responseData.getJSONArray("pekerja")
                                    val pekerjaList = parsePekerjaList(pekerjaArray)
                                    adapter.updateData(pekerjaList.toMutableList(), adminList.toMutableList())
                                    swipeRefreshLayout.isRefreshing = false
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

    @SuppressLint("SimpleDateFormat")
    fun convertStringToTime(timeStr: String): Time {
        val sdf = SimpleDateFormat("HH:mm:ss")
        val date: Date = sdf.parse(timeStr)
        return Time(date.time)
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

    private fun parsePekerjaList(pekerjaArray: JSONArray): List<Pekerja> {
        val pekerjaList = mutableListOf<Pekerja>()
        for (i in 0 until pekerjaArray.length()) {
            val pekerjaObject = pekerjaArray.getJSONObject(i)
            pekerjaList.add(
                Pekerja(
                    pekerjaObject.getInt("id"),
                    pekerjaObject.getInt("id_perusahaan"),
                    pekerjaObject.getString("email"),
                    pekerjaObject.getString("password"),
                    pekerjaObject.getString("nama"),
                    parseDate(pekerjaObject.getString("tanggal_lahir")),
                    pekerjaObject.getString("profile")
                )
            )
        }
        return pekerjaList
    }
    private fun parseDate(dateString: String): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }
    // Don't forget to cancel the requests in onDestroy to avoid memory leaks
    override fun onDestroy() {
        super.onDestroy()
        requestQueue.cancelAll(this)
        fetchRunnable?.let {
            handler.removeCallbacks(it)
        }
    }
    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                perusahaan = it.getParcelable("perusahaan")
                namaTv.setText(perusahaan?.nama)
                role = it.getString("role").toString()
                if(role == "Admin"){
                    admin = it.getParcelable("user")
                    addPekerja.visibility = View.VISIBLE
                }else{
                    pekerja = it.getParcelable("user")
                    addPekerja.visibility = View.GONE
                }
                val latitude = perusahaan?.latitude
                val longitude = perusahaan?.longitude
                val addressInfo = ReverseGeocoder.getAddressFromLocation(this@CompanyActivity, GeoPoint(
                    latitude!!, longitude!!
                ))
                if (addressInfo != null) {
                    val addressText = "${addressInfo.province}\n${addressInfo.country}"
                    address.text = addressText
                } else {
                    address.text = "Address information not available"
                }
                if (perusahaan?.logo != "null") {
                    val imageUrl =
                        "http://192.168.1.5:8000/api/Perusahaan/decryptLogo/${perusahaan?.id}"
                    val imageRequest = ImageRequest(
                        imageUrl,
                        { response ->
                            // Set the Bitmap to an ImageView or handle it as needed
                            val profileImageView = binding.circleImageView
                            profileImageView.setImageBitmap(response)
                        },
                        0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                        { error ->
                            error.printStackTrace()
                            Toast.makeText(this, "Failed to fetch profile image", Toast.LENGTH_SHORT).show()
                        }
                    )
                    val requestQueue = Volley.newRequestQueue(this)
                    requestQueue.add(imageRequest)
                }else{
                    val profileImageView = binding.circleImageView
                    Glide.with(this)
                        .load(R.drawable.logo)
                        .into(profileImageView)
                    val text = binding.tvName
                    text.setText(perusahaan?.nama)
                }
            }
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        // Remove any pending fetchRunnable instances from the handler
//        fetchRunnable?.let {
//            handler.removeCallbacks(it)
//        }
        if(pekerja != null){
            val userBundle = Bundle()
            val intent = Intent(this, UserActivity::class.java)
            userBundle.putParcelable("user", pekerja)
            userBundle.putParcelable("perusahaan", perusahaan)
            intent.putExtra("data", userBundle)
            startActivity(intent)
        }else{
            val userBundle = Bundle()
            val intent = Intent(this, AdminActivity::class.java)
            userBundle.putParcelable("user", admin)
            userBundle.putParcelable("perusahaan", perusahaan)
            intent.putExtra("data", userBundle)
            startActivity(intent)
        }
    }
}