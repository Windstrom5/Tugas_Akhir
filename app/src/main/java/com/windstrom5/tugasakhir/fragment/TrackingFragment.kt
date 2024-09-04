package com.windstrom5.tugasakhir.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.LocationUpdate
import com.windstrom5.tugasakhir.connection.LocationWebSocketService
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Perusahaan
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Date
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.connection.WorkHubs
import com.windstrom5.tugasakhir.model.Pekerja
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class TrackingFragment : Fragment() {
    private lateinit var webSocket: WebSocket
    private lateinit var mapView: MapView
    private lateinit var scarlet: Scarlet
    private lateinit var locationService: LocationWebSocketService
    private val markers = mutableMapOf<String, Marker>() // Map to store markers by user name
    private lateinit var requestQueue: RequestQueue
    private var perusahaan : Perusahaan? = null
    private var admin : Admin? = null
    private val pekerjaList = mutableListOf<Pekerja>()
    private val pollingInterval = 10000L
    private val handler = Handler(Looper.getMainLooper())
    private var fetchRunnable: Runnable? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tracking, container, false)
        mapView = view.findViewById(R.id.mapView)
        getBundle()
        initializeMap()
        requestQueue = Volley.newRequestQueue(requireContext())
        perusahaan?.let {
            fetchDataFromApi(it)
            focusOnPerusahaanLocation(it)
        }
//        val pusher = WorkHubs.pusher
//        val channel = pusher.subscribe("location-updates.${perusahaan?.nama}")
//        channel.bind("App\\Events\\LocationUpdated") { event ->
//            val data = event.data
//            val nama: String = data[0].toString()
//            val latitude: Double = data[1].toString().toDouble()
//            val longitude: Double = data[2].toString().toDouble()
//            val updated_at: String = data[3].toString()
//            val locationUpdate = LocationUpdate(nama, latitude, longitude, updated_at)
//            updateMapMarker(locationUpdate)
//        }
        return view
    }
    private fun fetchDataFromApi(perusahaan: Perusahaan) {
        val url = "http://192.168.1.6:8000/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        fetchRunnable = object : Runnable {
            override fun run() {
                val call = apiService.getLocationPekerja(perusahaan.nama)
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            response.body()?.let { responseBody ->
                                try {
                                    val absenDataArray = JSONArray(responseBody.string())
                                    // Iterate through the JSONArray and create LocationUpdate objects
                                    val locationUpdateList = mutableListOf<LocationUpdate>()
                                    for (i in 0 until absenDataArray.length()) {
                                        val absenObject = absenDataArray.getJSONObject(i)
                                        Log.d("json",absenObject.toString())
                                        val nama = absenObject.getString("nama")
                                        val latitude = absenObject.getDouble("latitude")
                                        val longitude = absenObject.getDouble("longitude")
                                        val updated_at = absenObject.getString("updated_at")
                                        val locationUpdate = LocationUpdate(nama, latitude, longitude, updated_at)
                                        locationUpdateList.add(locationUpdate)
                                    }
                                    // Update map markers with the location updates
                                    locationUpdateList.forEach { updateMapMarker(it) }
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

                // Schedule the next polling after the interval
                handler.postDelayed(this, pollingInterval)
            }
        }
        fetchRunnable?.let {
            handler.post(it)
        }
    }

    fun stopFetchRunnable() {
        fetchRunnable?.let {
            handler.removeCallbacks(it)
        }
    }
//    private fun getPekerja(perusahaan: Perusahaan) {
//        val apiUrl = "http://192.168.1.6:8000/api/getPekerja/${perusahaan.nama}"
//
//        val jsonObjectRequest = JsonObjectRequest(
//            Request.Method.GET, apiUrl, null,
//            { response ->
//                try {
//                    val pekerjaArray = response.getJSONArray("pekerja") // Assuming your response has a "pekerja" array
//                    for (i in 0 until pekerjaArray.length()) {
//                        val pekerjaObject = pekerjaArray.getJSONObject(i)
//                        val pekerja = Pekerja(
//                            null,
//                            pekerjaObject.getInt("id_perusahaan"),
//                            pekerjaObject.getString("email"),
//                            pekerjaObject.getString("password"),
//                            pekerjaObject.getString("nama"),
//                            Date(pekerjaObject.getLong("tanggal_lahir")),
//                            pekerjaObject.getString("profile")
//                        )
//                        pekerjaList.add(pekerja)
//                    }
//
//                    // Now the pekerjaList contains all the fetched Pekerja data
//                    // You can use pekerjaList as needed
//                } catch (e: JSONException) {
//                    e.printStackTrace()
//                }
//            },
//            { error ->
//                // Handle error cases
//                error.printStackTrace()
//            })
//
//        requestQueue.add(jsonObjectRequest)
//    }
    private fun getBundle() {
        val arguments = arguments
        if (arguments != null) {
            perusahaan = arguments.getParcelable("perusahaan")
            admin = arguments.getParcelable("user")
        } else {
            Log.d("Error","Bundle Not Found")
        }
    }
    private fun initializeMap() {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(requireContext()))
        Configuration.getInstance().userAgentValue = "MyCustomApp/1.0"
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.isTilesScaledToDpi = true
        mapView.setMultiTouchControls(true)
    }
    private fun focusOnPerusahaanLocation(perusahaan: Perusahaan) {
        val perusahaanLocation = GeoPoint(perusahaan.latitude, perusahaan.longitude)
        mapView.controller.setCenter(perusahaanLocation)
        mapView.controller.setZoom(14.0)
    }
    private fun updateMapMarker(locationUpdate: LocationUpdate) {
        val userName = locationUpdate.Nama
        val latitude = locationUpdate.latitude
        val longitude = locationUpdate.longitude
        val update = locationUpdate.update_at

        // Parsing the update time to Date
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val updateTime: Date?
        try {
            updateTime = formatter.parse(update)
        } catch (e: ParseException) {
            e.printStackTrace()
            return
        }

        val currentTime = Date()

        // Calculating the difference in time
        val diffInMillis = currentTime.time - updateTime!!.time
        val diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
        val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60

        val timeAgo = when {
            diffInHours > 0 -> "$diffInHours hours ago"
            diffInMinutes > 0 -> "$diffInMinutes minutes ago"
            else -> "Just now"
        }

        val geoPoint = GeoPoint(latitude, longitude)

        if (markers.containsKey(userName)) {
            // Update the position of the existing marker
            val marker = markers[userName]
            marker?.position = geoPoint
            marker?.subDescription = "Last updated: $timeAgo"
        } else {
            // If the marker for the user doesn't exist, create a new one
            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.title = userName
            marker.subDescription = "Last updated: $timeAgo"
            mapView.overlays.add(marker)
            markers[userName] = marker
        }

        mapView.invalidate()
    }
    private fun updateMarkerWithApiRequest(userName: String, latitude: Double, longitude: Double) {
        if (!markers.containsKey(userName)) {
            // If the marker for the user doesn't exist, create a new one
            val marker = Marker(mapView)
            marker.position = GeoPoint(latitude, longitude)
            marker.title = userName
            mapView.overlays.add(marker)
            markers[userName] = marker
        } else {
            // Update the position of the existing marker
            val marker = markers[userName]
            marker?.position = GeoPoint(latitude, longitude)
        }
        mapView.invalidate()
    }
    private fun updateLocation(perusahaan: Perusahaan) {
        // Send request to server to get user's locations
        val serverUrl = "http://192.168.1.6:8000/api/Presensi/getLocation/${perusahaan.nama}"
        val request = JsonArrayRequest(
            Request.Method.GET, serverUrl, null,
            { response ->
                // Handle the response from the server
                try {
                    // Loop through the array of user locations
                    for (i in 0 until response.length()) {
                        val userObj = response.getJSONObject(i)
                        val userLatitude = userObj.getDouble("latitude")
                        val userLongitude = userObj.getDouble("longitude")
                        val nama = userObj.getString("nama")
                        val update_at = userObj.getString("update_at")
                        val locationUpdate = LocationUpdate(nama, userLatitude, userLongitude,update_at)
                        updateMapMarker(locationUpdate)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Handle error
                error.printStackTrace()
            }
        )

        // Add the request to the RequestQueue
        requestQueue.add(request)
    }
    override fun onPause() {
        super.onPause()

        // Stop the periodic update task when the fragment is paused
        handler.removeCallbacks(updateRunnable)
    }
    private val updateRunnable = object : Runnable {
        override fun run() {
            // Perform location update and marker update here
            perusahaan?.let { updateLocation(it) }

            // Repeat the task after the update interval
            handler.postDelayed(this, pollingInterval)
        }
    }
//        initializeWebSocket()
//    override fun onDestroy() {
//        super.onDestroy()
//    }

    //    private fun initializeWebSocket() {
//        val ngrokUrl = "https://1490-36-80-222-40.ngrok-free.app"
//        val webSocketUrl = "$ngrokUrl/broadcasting"
//
//        val okHttpClient = OkHttpClient.Builder().build()
//        scarlet = Scarlet.Builder()
//            .webSocketFactory(okHttpClient.newWebSocketFactory(webSocketUrl))
//            .addMessageAdapterFactory(MoshiMessageAdapter.Factory())
//            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
//            .build()
//
//        locationService = scarlet.create<LocationWebSocketService>()
//
//        locationService.observeLocationUpdates()
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { locationUpdate ->
//                updateMapMarker(locationUpdate)
//            }
//    }
}
