package com.windstrom5.tugasakhir.activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import com.windstrom5.tugasakhir.databinding.ActivityMapBinding
import com.windstrom5.tugasakhir.location.CustomInfoWindow
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import java.util.Locale
import android.location.Address as AndroidLocationAddress

class MapActivity : AppCompatActivity(){
    private lateinit var mapController: IMapController
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var mapView: MapView
    private var bundle: Bundle? = null
    private lateinit var binding: ActivityMapBinding
    private lateinit var save : ExtendedFloatingActionButton
    private lateinit var myLocationMarker: Marker
    private lateinit var locationManager: LocationManager
    private lateinit var tvLatitudeLongitude : TextView
    private lateinit var selectedLocationMarker: Marker
    private lateinit var tvAddress : TextView
    private lateinit var defaultMarker: Marker
    private var latitude : Double = 0.0
    private var longitude : Double = 0.0
    private var namaPerusahaan : String? = null
    private var openhour : String? = null
    private var closehour : String? = null
    private lateinit var address : String
    private lateinit var tvPerusahaan : TextView
    private lateinit var openClose:TextView
    private lateinit var cardview : CardView
    private lateinit var category: String
    private var role:String ?=null
    private var admin : Admin? = null
    private var pekerja : Pekerja? = null
    private var perusahaan: Perusahaan?= null
    private var byteArray:ByteArray?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue("MyCustomApp/1.0");
        save = binding.saveButton
        tvLatitudeLongitude = binding.tvLatitudeLongitude
        tvAddress = binding.tvAddress
        tvPerusahaan = binding.tvNamaPerusahaan
        save.visibility = View.INVISIBLE
        mapView = binding.mapView
        openClose = binding.tvOpenHour
        getBundle()
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(false)
        mapView.setMultiTouchControls(true)
        cardview = binding.infoCardView
        cardview.visibility = View.INVISIBLE
        mapController = mapView.controller
        mapController.setZoom(20.0)
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        mapView.overlays.add(locationOverlay)
        locationOverlay.enableMyLocation()
        selectedLocationMarker = Marker(mapView)
        myLocationMarker = Marker(mapView)
        defaultMarker = Marker(mapView)
        defaultMarker.title = "Selected Location"
        myLocationMarker.icon = resources.getDrawable(R.drawable.location) // Replace with your person logo drawable
        myLocationMarker.title = "My Location"
        val myLocationInfoWindow = CustomInfoWindow(R.layout.custom_info_window, mapView, myLocationMarker.title,myLocationMarker)
        myLocationMarker.infoWindow = myLocationInfoWindow
        mapView.overlays.add(myLocationMarker)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (category == "edit") {
            val perusahaanLatitude = perusahaan?.latitude ?: 0.0
            val perusahaanLongitude = perusahaan?.longitude ?: 0.0

            // Center the map on the Perusahaan location
            val perusahaanLocation = GeoPoint(perusahaanLatitude, perusahaanLongitude)
            mapController.animateTo(perusahaanLocation)
            val companyMarker = Marker(mapView)
            companyMarker.position = GeoPoint(perusahaanLatitude, perusahaanLongitude)
            companyMarker.title = perusahaan?.nama
            val companyInfoWindow = CustomInfoWindow(
                R.layout.custom_info_window,
                mapView,
                perusahaan?.nama ?: "", // Company name to be displayed
                companyMarker
            )
            companyMarker.infoWindow = companyInfoWindow

// Add the company marker to the map overlays
            mapView.overlays.add(companyMarker)
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Request location updates
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0,
                    0f,
                    locationListener
                )

                // Get last known location
                val lastKnownLocation =
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnownLocation != null) {
                    // Center the map on the last known location
                    val userLocation = GeoPoint(lastKnownLocation)
                    mapController.animateTo(userLocation)

                    // Add a marker at the user's location
                    myLocationMarker.position = userLocation
                    mapView.invalidate() // Refresh the map to update the marker position

                    // Get the map center
                    val mapCenter = mapView.mapCenter
                    // Add a marker at the center of the screen
                    selectedLocationMarker.position =
                        GeoPoint(mapCenter.latitude, mapCenter.longitude)
                    mapView.invalidate() // Refresh the map to update the marker position
                }
            }
        }
        save.setOnClickListener {
            if(category == "edit"){
                val intent = Intent(this, EditCompany::class.java)
                val Bundle = Bundle()
                Bundle.putParcelable("perusahaan",perusahaan)
                if(role == "Admin"){
                    Bundle.putParcelable("user", admin)
                }else{
                    Bundle.putParcelable("user", pekerja)
                }
                Bundle.putString("role",role)
                Bundle.putString("namaPerusahaan", namaPerusahaan?: "")
                Bundle.putDouble("latitude", latitude)
                Bundle.putDouble("longitude", longitude)
                Bundle.putString("openhour", openhour)
                Bundle.putString("closehour", closehour)
                Bundle.putString("address", address)
                if(byteArray != null){
                    Bundle.putByteArray("selectedFile", byteArray)
                }
                intent.putExtra("edit", Bundle)
                startActivity(intent)
            }else{
                val intent = Intent(this, RegisterActivity::class.java)
                val Bundle = Bundle()
                Bundle.putString("namaPerusahaan", namaPerusahaan?: "")
                Bundle.putDouble("latitude", latitude)
                Log.d("Editor",latitude.toString())
                Bundle.putDouble("longitude", longitude)
                Bundle.putString("openhour", openhour)
                Bundle.putString("closehour", closehour)
                Bundle.putString("address", address)
                if(byteArray != null){
                    Bundle.putByteArray("selectedFile", byteArray)
                }
                intent.putExtra("data", Bundle)
                startActivity(intent)
            }

//            finish()
//            // Handle save button click
//            // You can retrieve latitude, longitude, and address here
//            val latitude = mapView.boundingBox.centerLatitude
//            val longitude = mapView.boundingBox.centerLongitude
//            // Remove existing marker if present
//            selectedLocationMarker = Marker(mapView)
//            selectedLocationMarker.let { mapView.overlays.remove(it) }
//            // Add a marker to the selected location
//            selectedLocationMarker.position = GeoPoint(latitude, longitude)
//            selectedLocationMarker.title = "Selected Location"
//            val selectedLocationInfoWindow = CustomInfoWindow(R.layout.custom_info_window, mapView, selectedLocationMarker.title, selectedLocationMarker)
//            selectedLocationMarker.infoWindow = selectedLocationInfoWindow
//
//            mapView.overlays.add(selectedLocationMarker)
        }
        // Set up the map click listener to make the save button visible
        mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    // User clicked on the map, make Save button and CardView visible
                    save.visibility = View.VISIBLE
                    cardview.visibility = View.VISIBLE
                    latitude = it.latitude
                    longitude = it.longitude
                    // Fetch and display address information
                    getAddressFromLocation(it.latitude, it.longitude)

                    // Remove existing markers
//                    mapView.overlays.remove(myLocationMarker)
                    mapView.overlays.remove(selectedLocationMarker)

                    // Add a marker to the selected location
                    selectedLocationMarker.position = GeoPoint(it.latitude, it.longitude)
                    selectedLocationMarker.title = "Selected Location"
                    mapView.overlays.add(selectedLocationMarker)

                    // Move the default marker to the tapped location
                    defaultMarker.position = GeoPoint(it.latitude, it.longitude)
                    mapView.invalidate()

                    return true
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                // Handle long press if needed
                return false
            }
        }))
    }
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Move map to the new location
            mapController.animateTo(GeoPoint(location))
        }

        override fun onProviderDisabled(provider: String) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    }
    override fun onBackPressed() {
        super.onBackPressed()
        if(category == "edit"){
            val userBundle = Bundle()
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }else{
            val userBundle = Bundle()
            val intent = Intent(this, AdminActivity::class.java)
            if(role == "Admin"){
                userBundle.putParcelable("user", admin)
            }else{
                userBundle.putParcelable("user", pekerja)
            }
            if(byteArray != null){
                userBundle.putByteArray("selectedFile", byteArray)
            }
            userBundle.putParcelable("perusahaan", perusahaan)
            intent.putExtra("data", userBundle)
            startActivity(intent)
        }
    }
    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<AndroidLocationAddress> = geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
            if (addresses.isNotEmpty()) {
                val Address: AndroidLocationAddress = addresses[0]
                address = Address.getAddressLine(0)
                tvLatitudeLongitude.text = "Latitude: $latitude, Longitude: $longitude"
                tvAddress.text = formatAddress(Address)
                if(namaPerusahaan != null){
                    tvPerusahaan.text = namaPerusahaan
                }else{
                    tvPerusahaan.visibility = View.GONE
                }
                if (openhour != null && closehour != null) {
                    openClose.text = "Open Hours = $openhour - $closehour"
                } else if (openhour != null) {
                    openClose.text = "Open Hours = $openhour"
                } else if (closehour != null) {
                    openClose.text = "Close Hours = $closehour"
                } else {
                    openClose.visibility = View.GONE
                }
            } else {
                tvAddress.text = "Address: Not Available"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            tvAddress.text = "Address: Not Available"
        }
    }
    private fun formatAddress(address: AndroidLocationAddress?): String {
        address ?: return ""
        val formattedAddress = StringBuilder()

        // Loop through address lines
        for (i in 0 until address.maxAddressLineIndex + 1) {
            formattedAddress.append(address.getAddressLine(i))
            if (i < address.maxAddressLineIndex) {
                formattedAddress.append(", ")
            }
        }

        // Additional components if available
        val locality = address.locality
        val postalCode = address.postalCode
        val countryName = address.countryName

        if (!locality.isNullOrBlank()) {
            formattedAddress.append(", ").append(locality)
        }
        if (!postalCode.isNullOrBlank()) {
            formattedAddress.append(", ").append(postalCode)
        }
        if (!countryName.isNullOrBlank()) {
            formattedAddress.append(", ").append(countryName)
        }

        return formattedAddress.toString()
    }

    private fun getBundle() {
        bundle = intent?.getBundleExtra("data")
        if (bundle != null) {
            bundle?.let {
                category = it.getString("edit").toString()
                Log.d("editor",category)
                if(category == "edit"){
                    role = it.getString("role")
                    perusahaan = it.getParcelable("perusahaan")!!
                    namaPerusahaan = it.getString("namaPerusahaan") ?: ""
                    openhour = it.getString("openhour") ?: ""
                    closehour = it.getString("closehour") ?: ""
                    if(role == "Admin"){
                        admin = it.getParcelable("user")
                    }else{
                        pekerja = it.getParcelable("user")
                    }
                    byteArray = intent.getByteArrayExtra("selectedFile")
                }else{
                    namaPerusahaan = it.getString("namaPerusahaan") ?: ""
                    openhour = it.getString("openhour") ?: ""
                    closehour = it.getString("closehour") ?: ""
                    byteArray = intent.getByteArrayExtra("selectedFile")
                }
            }
        } else {
            // Handle the case when bundle is null
        }
    }
}