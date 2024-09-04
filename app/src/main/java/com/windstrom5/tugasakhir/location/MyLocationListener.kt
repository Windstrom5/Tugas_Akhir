package com.windstrom5.tugasakhir.location

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import org.osmdroid.util.GeoPoint

class MyLocationListener(private val callback: (GeoPoint) -> Unit) : LocationListener {

    override fun onLocationChanged(location: Location) {
        val currentLocation = GeoPoint(location.latitude, location.longitude)
        callback.invoke(currentLocation)
    }

    override fun onProviderDisabled(provider: String) {
        // Handle provider disabled
    }

    override fun onProviderEnabled(provider: String) {
        // Handle provider enabled
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {
        // Handle status changed
    }
}
