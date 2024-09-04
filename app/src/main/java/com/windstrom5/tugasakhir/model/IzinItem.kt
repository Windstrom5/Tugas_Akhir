package com.windstrom5.tugasakhir.model

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

data class IzinItem(
    val id: Int? = null,
    val id_perusahaan: Int?= null,
    val id_pekerja: Int?=null,
    val nama_pekerja: String,
    val nama_perusahaan: String,
    val tanggal: Date,
    val kategori: String,
    val alasan: String,
    val bukti: String,
    val status: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readSerializable() as Date,
        parcel.readString() ?: "",
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
        parcel.writeLong(tanggal.time)
        parcel.writeString(kategori)
        parcel.writeString(alasan)
        parcel.writeString(bukti)
        parcel.writeString(bukti)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<IzinItem> {
        override fun createFromParcel(parcel: Parcel): IzinItem {
            return IzinItem(parcel)
        }

        override fun newArray(size: Int): Array<IzinItem?> {
            return arrayOfNulls(size)
        }
    }
}