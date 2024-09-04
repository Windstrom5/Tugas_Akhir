package com.windstrom5.tugasakhir.connection

import android.content.Context
import android.location.Geocoder
import android.widget.Toast
import org.osmdroid.util.GeoPoint
import java.io.IOException
import java.util.Locale


data class AddressInfo(val province: String?, val country: String?)

object ReverseGeocoder {
    fun getAddressFromLocation(context: Context?, geoPoint: GeoPoint): AddressInfo? {
        val geocoder = Geocoder(context!!, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(
                geoPoint.latitude,
                geoPoint.longitude,
                1
            ) // Specify the maximum number of results to be returned
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val province = address.adminArea // Get the province
                val country = address.countryName // Get the country
                return AddressInfo(province, country)
            } else {
                // Handle case where no address was found
                Toast.makeText(
                    context,
                    "No address found for the provided coordinates",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle IOException
            Toast.makeText(context, "IOException occurred", Toast.LENGTH_LONG).show()
        }
        return null
    }

    fun getFullAddressFromLocation(context: Context?, geoPoint: GeoPoint): String? {
        val geocoder = Geocoder(context!!, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(
                geoPoint.latitude,
                geoPoint.longitude,
                1
            ) // Specify the maximum number of results to be returned
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val addressParts = mutableListOf<String>()

                // Add address components to the list
                addressParts.add(address.getAddressLine(0)) // Street address
                addressParts.add(address.locality) // Locality
                addressParts.add(address.adminArea) // Province
                addressParts.add(address.countryName) // Country
                addressParts.add(address.postalCode) // Postal code

                // Concatenate address parts into a single string
                return addressParts.joinToString(", ")
            } else {
                // Handle case where no address was found
                Toast.makeText(
                    context,
                    "No address found for the provided coordinates",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle IOException
            Toast.makeText(context, "IOException occurred", Toast.LENGTH_LONG).show()
        }
        return null
    }

}


