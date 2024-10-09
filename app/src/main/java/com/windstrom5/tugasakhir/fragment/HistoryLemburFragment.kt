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
import androidx.compose.ui.semantics.Role
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.adapter.IzinAdapter
import com.windstrom5.tugasakhir.adapter.LemburAdapter
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.IzinItem
import com.windstrom5.tugasakhir.model.LemburItem
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import com.windstrom5.tugasakhir.model.historyIzin
import com.windstrom5.tugasakhir.model.historyLembur
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryLemburFragment : Fragment(){
    private lateinit var expandableListView: ExpandableListView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: LemburAdapter
    private var perusahaan : Perusahaan? = null
    private var admin : Admin? = null
    private var pekerja : Pekerja? = null
    private var role : String? = null
    private var fetchRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private val pollingInterval = 2000L
    private val filteredList = mutableListOf<historyLembur>() // For storing filtered data
    private lateinit var searchEditText: EditText
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history_lembur, container, false)
        getBundle()
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        adapter = perusahaan?.let { LemburAdapter(it,requireContext(), filteredList, role ?: "") }!!
        expandableListView = view.findViewById(R.id.expandableListView)
        expandableListView.setAdapter(adapter)
        Log.d("FetchDataAdmin", admin.toString())
        Log.d("FetchDataAdmin", pekerja.toString())
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
    private fun fetchDataPekerjaFromApi(namaPerusahaan: String, nama_pekerja: String) {
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        val call = apiService.getDataLemburPekerja(namaPerusahaan, nama_pekerja)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")
                            val statusWithLemburMap = mutableMapOf<String, MutableList<LemburItem>>()

                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val status = jsonObject.getString("status")
                                val tanggal = jsonObject.getString("tanggal")
                                val tanggalDate = parseDate(tanggal)
                                val lembur = LemburItem(
                                    jsonObject.getInt("id"),
                                    jsonObject.getInt("id_perusahaan"),
                                    jsonObject.getInt("id_pekerja"),
                                    jsonObject.getString("nama_pekerja"),
                                    jsonObject.getString("nama_perusahaan"),
                                    tanggalDate,
                                    Time.valueOf(jsonObject.getString("waktu_masuk")),
                                    Time.valueOf(jsonObject.getString("waktu_pulang")),
                                    jsonObject.getString("pekerjaan"),
                                    jsonObject.getString("bukti"),
                                    jsonObject.getString("status")
                                )
                                if (statusWithLemburMap.containsKey(status)) {
                                    statusWithLemburMap[status]?.add(lembur)
                                } else {
                                    statusWithLemburMap[status] = mutableListOf(lembur)
                                }
                            }

                            val statusWithLemburList = statusWithLemburMap.map { entry ->
                                historyLembur(entry.key, entry.value)
                            }

                            val expandableListView = view?.findViewById<ExpandableListView>(R.id.expandableListView)
                            val adapter = expandableListView?.adapter as? LemburAdapter
                            Log.d("FetchData", role.toString())
                            if (adapter != null) {
                                Log.d("FetchData", "Clearing old data")
                                adapter.clearData() // Clear old data
                                Log.d("FetchData", "Updating new data")
                                adapter.updateData(statusWithLemburList) // Set new data
                            } else {
                                Log.d("FetchData", "Setting new adapter")
                                val newAdapter = perusahaan?.let {
                                    LemburAdapter(
                                        it,
                                        requireContext(),
                                        statusWithLemburList,
                                        "Pekerja"
                                    )
                                }
                                expandableListView?.setAdapter(newAdapter)
                            }

                            swipeRefreshLayout.isRefreshing = false
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                            swipeRefreshLayout.isRefreshing = false
                        }
                    }
                } else {
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }


    private fun fetchDataPerusahaanFromApi(namaPerusahaan: String) {
        val url = "https://selected-jaguar-presently.ngrok-free.app/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
//        fetchRunnable = object : Runnable {
//            override fun run() {
        val call = apiService.getDataLemburPerusahaan(namaPerusahaan)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val jsonResponse = responseBody.string()
                            val responseData = JSONObject(jsonResponse)
                            val dataArray = responseData.getJSONArray("data")
                            val statusWithLemburMap = mutableMapOf<String, MutableList<LemburItem>>()
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            for (i in 0 until dataArray.length()) {
                                val jsonObject = dataArray.getJSONObject(i)
                                val status = jsonObject.getString("status")
                                val tanggalString = jsonObject.getString("tanggal")
                                val tanggalDate = parseDate(tanggalString)
                                val lembur = LemburItem(
                                    jsonObject.getInt("id"),
                                    jsonObject.getInt("id_perusahaan"),
                                    jsonObject.getInt("id_pekerja"),
                                    jsonObject.getString("nama_pekerja"),
                                    jsonObject.getString("nama_perusahaan"),
                                    tanggalDate,
                                    Time.valueOf(jsonObject.getString("waktu_masuk")),
                                    Time.valueOf(jsonObject.getString("waktu_pulang")),
                                    jsonObject.getString("pekerjaan"),
                                    jsonObject.getString("bukti"),
                                    jsonObject.getString("status")
                                )
                                if (statusWithLemburMap.containsKey(status)) {
                                    statusWithLemburMap[status]?.add(lembur)
                                } else {
                                    statusWithLemburMap[status] = mutableListOf(lembur)
                                }
                            }
                            val statusWithLemburList = statusWithLemburMap.map { entry ->
                                historyLembur(entry.key, entry.value)
                            }

                            // Populate ExpandableListView with data
                            val expandableListView = view?.findViewById<ExpandableListView>(R.id.expandableListView)
                            val adapter = expandableListView?.adapter as? LemburAdapter

                            if (adapter != null) {
                                Log.d("FetchData", "Clearing old data")
                                adapter.clearData() // Clear old data
                                Log.d("FetchData", "Updating new data")
                                adapter.updateData(statusWithLemburList) // Set new data
                            } else {
                                Log.d("FetchData", "Setting new adapter")
                                val newAdapter = perusahaan?.let {
                                    LemburAdapter(
                                        it,
                                        requireContext(),
                                        statusWithLemburList,
                                        "Admin"
                                    )
                                }
                                expandableListView?.setAdapter(newAdapter)
                            }
                            swipeRefreshLayout.isRefreshing = false
                        } catch (e: JSONException) {
                            Log.e("FetchDataError", "Error parsing JSON: ${e.message}")
                            swipeRefreshLayout.isRefreshing = false
                        }
                    }
                } else {
                    // Handle unsuccessful response
                    Log.e("FetchDataError", "Failed to fetch data: ${response.code()}")
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // Handle network failures
                Log.e("FetchDataError", "Failed to fetch data: ${t.message}")
            }
        })
    }
    private fun parseDate(dateString: String): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }
    private fun getBundle() {
        val arguments = arguments
        if (arguments != null) {
            perusahaan = arguments.getParcelable("perusahaan")
            role = arguments.getString("role")
            Log.d("role",role.toString())
            if(role.toString() == "Admin"){
                admin = arguments.getParcelable("user")
            }else{
                pekerja = arguments.getParcelable("user")
//                perusahaan?.let { pekerja?.let { it1 -> fetchDataFromApiPekerja(it.nama, it1.nama) } }
            }
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }
}