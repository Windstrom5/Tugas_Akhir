package com.windstrom5.tugasakhir.model

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

class DinasItem(
    val id: Int? = null,
    val id_perusahaan: Int?= null,
    val id_pekerja: Int?=null,
    val nama_pekerja: String,
    val nama_perusahaan: String,
    val tujuan: String,
    val tanggal_berangkat: Date,
    val tanggal_pulang: Date,
    val kegiatan: String,
    val bukti: String,
    val status: String
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readSerializable() as Date,
        parcel.readSerializable() as Date,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeValue(id_perusahaan)
        parcel.writeValue(id_pekerja)
        parcel.writeString(nama_pekerja)
        parcel.writeString(nama_pekerja)
        parcel.writeString(tujuan)
        parcel.writeSerializable(tanggal_berangkat)
        parcel.writeSerializable(tanggal_pulang)
        parcel.writeString(kegiatan)
        parcel.writeString(bukti)
        parcel.writeString(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DinasItem> {
        override fun createFromParcel(parcel: Parcel): DinasItem {
            return DinasItem(parcel)
        }

        override fun newArray(size: Int): Array<DinasItem?> {
            return arrayOfNulls(size)
        }
    }
}