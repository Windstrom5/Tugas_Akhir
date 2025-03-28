package com.windstrom5.tugasakhir.model

import android.os.Parcel
import android.os.Parcelable
import java.sql.Time
import java.util.Date

data class LemburItem(
    val id: Int? = null,
    val id_perusahaan: Int?= null,
    val id_pekerja: Int?=null,
    val nama_pekerja: String,
    val nama_perusahaan: String,
    val tanggal: Date, // Assuming you handle dates as strings
    val waktu_masuk: Time, // Assuming you handle times as strings
    val waktu_pulang: Time,
    val pekerjaan: String,
    val bukti: String,
    var status: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readSerializable() as Date,
        Time.valueOf(parcel.readString() ?: "00:00:00"),
        Time.valueOf(parcel.readString() ?: "00:00:00"),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeValue(id_perusahaan)
        parcel.writeValue(id_pekerja)
        parcel.writeString(nama_pekerja)
        parcel.writeString(nama_perusahaan)
        parcel.writeSerializable(tanggal)
        parcel.writeString(waktu_masuk.toString())
        parcel.writeString(waktu_pulang.toString())
        parcel.writeString(pekerjaan)
        parcel.writeString(bukti)
        parcel.writeString(bukti)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LemburItem> {
        override fun createFromParcel(parcel: Parcel): LemburItem {
            return LemburItem(parcel)
        }

        override fun newArray(size: Int): Array<LemburItem?> {
            return arrayOfNulls(size)
        }
    }
}