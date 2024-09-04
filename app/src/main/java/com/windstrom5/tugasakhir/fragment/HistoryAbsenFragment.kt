package com.windstrom5.tugasakhir.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ExpandableListView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.adapter.AbsenAdapter
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.model.Absen
import com.windstrom5.tugasakhir.model.AbsenItem
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.historyAbsen
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.sql.Date
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Locale


class HistoryAbsenFragment : Fragment() {
    private lateinit var expandableListView: ExpandableListView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: AbsenAdapter
    private var perusahaan : Perusahaan? = null
    private var admin : Admin? = null
    private var pekerja : Pekerja? = null
    private var role : String? = null
    private var fetchRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private val pollingInterval = 2000L
    private lateinit var searchEditText: EditText
    private var statusWithAbsenList: List<historyAbsen>? = null
    private val filteredList = mutableListOf<historyAbsen>() // For storing filtered data
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history_absen, container, false)
        getBundle()
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        expandableListView = view.findViewById(R.id.expandableListView)
        adapter = perusahaan?.let { AbsenAdapter(it,requireContext(), filteredList, role ?: "") }!!
        expandableListView.setAdapter(adapter)
        if(admin != null){
            perusahaan?.let { fetchDataPerusahaanFromApi(it.nama) }
            swipeRefreshLayout.setOnRefreshListener {
                perusahaan?.let { fetchDataPerusahaanFromApi(it.nama) }
            }
        }else{
            perusahaan?.let { pekerja?.let { it1 -> fetchDataPekerjaFromApi(it.nama, it1.nama) } }
            swipeRefreshLayout.setOnRefreshListener {
                perusahaan?.let { pekerja?.let { it1 -> fetchDataPekerjaFromApi(it.nama, it1.nama) } }
            }
        }
        searchEditText = view.findViewById(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                // Filter the data in the adapter based on the query
                adapter.filterData(query)
            }
        })
        return view
    }

    private fun fetchDataPekerjaFromApi(namaPerusahaan: String,nama_pekerja: String) {
        val url = "http://192.168.1.6:8000/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val call = apiService.getDataAbsenPekerja(namaPerusahaan,nama_pekerja)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")
                            // Initialize a map to hold status with corresponding Absen list
                            val statusWithAbsenMap = mutableMapOf<String, MutableList<AbsenItem>>()

                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val status: String
                                val keluar: Time?

                                if (jsonObject.has("jam_keluar")) {
                                    keluar = Time.valueOf(jsonObject.getString("jam_keluar"))
                                    status = "Completed"
                                } else {
                                    keluar = null
                                    status = "OnGoing"
                                }
                                val Absen = AbsenItem(
                                    id = jsonObject.getInt("id"),
                                    id_perusahaan = jsonObject.getInt("id_perusahaan"),
                                    id_pekerja = jsonObject.getInt("id_pekerja"),
                                    nama_pekerja = jsonObject.getString("nama_pekerja"),
                                    nama_perusahaan = jsonObject.getString("nama_perusahaan"),
                                    tanggal = Date(jsonObject.getLong("tanggal")),
                                    masuk = Time.valueOf(jsonObject.getString("jam_masuk")),
                                    keluar = keluar
                                )
                                // Add Absen to corresponding status list
                                if (statusWithAbsenMap.containsKey(status)) {
                                    statusWithAbsenMap[status]?.add(Absen)
                                } else {
                                    statusWithAbsenMap[status] = mutableListOf(Absen)
                                }
                            }

                            // Convert the map to a list of StatusWithAbsen objects
                            statusWithAbsenList = statusWithAbsenMap.map { entry ->
                                historyAbsen(entry.key, entry.value)
                            }

                            // Populate ExpandableListView with data
                            adapter = perusahaan?.let {
                                AbsenAdapter(
                                    it,
                                    requireContext(),
                                    statusWithAbsenList!!,
                                    "Pekerja"
                                )
                            }!!
                            expandableListView.setAdapter(adapter)
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

    private fun fetchDataPerusahaanFromApi(namaPerusahaan: String) {
        val url = "http://192.168.1.6:8000/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.getDataAbsenPerusahaan(namaPerusahaan)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")

                            // Initialize a map to hold status with corresponding Absen list
                            val statusWithAbsenMap = mutableMapOf<String, MutableList<AbsenItem>>()

                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val status: String
                                val keluar: Time?

                                if (jsonObject.has("jam_keluar")) {
                                    keluar = Time.valueOf(jsonObject.getString("jam_keluar"))
                                    status = "Completed"
                                } else {
                                    keluar = null
                                    status = "OnGoing"
                                }

                                val Absen = AbsenItem(
                                    id = jsonObject.getInt("id"),
                                    id_perusahaan = jsonObject.getInt("id_perusahaan"),
                                    id_pekerja = jsonObject.getInt("id_pekerja"),
                                    nama_pekerja = jsonObject.getString("nama_pekerja"),
                                    nama_perusahaan = jsonObject.getString("nama_perusahaan"),
                                    tanggal = Date(jsonObject.getLong("tanggal")),
                                    masuk = Time.valueOf(jsonObject.getString("jam_masuk")),
                                    keluar = keluar
                                )

                                // Add Absen to corresponding status list
                                if (statusWithAbsenMap.containsKey(status)) {
                                    statusWithAbsenMap[status]?.add(Absen)
                                } else {
                                    statusWithAbsenMap[status] = mutableListOf(Absen)
                                }
                            }

                            // Convert the map to a list of StatusWithAbsen objects
                            statusWithAbsenList = statusWithAbsenMap.map { entry ->
                                historyAbsen(entry.key, entry.value)
                            }

                            // Populate ExpandableListView with data
                            adapter = perusahaan?.let {
                                AbsenAdapter(
                                    it,
                                    requireContext(),
                                    statusWithAbsenList!!,
                                    "Pekerja"
                                )
                            }!!
                            expandableListView.setAdapter(adapter)
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

//
//                // Schedule the next polling after the interval
//                handler.postDelayed(this, pollingInterval)
//            }
//        }
//        // Start the initial polling
//        fetchRunnable?.let {
//            handler.post(it)
//        }
//    }
    fun parseDate(dateString: String): java.util.Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.parse(dateString) ?: java.util.Date()
    }
    private fun getBundle() {
        val arguments = arguments
        if (arguments != null) {
            perusahaan = arguments.getParcelable("perusahaan")
            role = arguments.getString("role")
            if(role.toString() == "Admin"){
                admin = arguments.getParcelable("user")
            }else{
                pekerja = arguments.getParcelable("user")
            }
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }
}
