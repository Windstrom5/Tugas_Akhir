package com.windstrom5.tugasakhir.model

import android.os.Parcel
import android.os.Parcelable
import java.sql.Date
import java.sql.Time

data class Perusahaan(
    val id: Int? = null,
    val nama: String,
    val latitude: Double,
    val longitude: Double,
    val jam_masuk: Time,
    val jam_keluar: Time,
    val batasAktif: Date,
    val logo: String?,
    val secret_key: String,
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        Time.valueOf(parcel.readString() ?: "00:00:00"),
        Time.valueOf(parcel.readString() ?: "00:00:00"),
        parcel.readSerializable() as Date,
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(nama)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeString(jam_masuk.toString())
        parcel.writeString(jam_keluar.toString())
        parcel.writeSerializable(batasAktif)
        parcel.writeString(logo)
        parcel.writeString(secret_key)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Perusahaan> {
        override fun createFromParcel(parcel: Parcel): Perusahaan {
            return Perusahaan(parcel)
        }

        override fun newArray(size: Int): Array<Perusahaan?> {
            return arrayOfNulls(size)
        }
    }
}