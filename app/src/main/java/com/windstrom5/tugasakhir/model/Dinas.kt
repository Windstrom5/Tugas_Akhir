package com.windstrom5.tugasakhir.model

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

class Dinas(
    val id: Int? = null,
    val id_pekerja: Int,
    val id_perusahaan: Int,
    val tujuan: String,
    val tanggal_berangkat: Date,
    val tanggal_pulang: Date,
    val kegiatan: String,
    val bukti: String,
    val status: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readSerializable() as Date,
        parcel.readSerializable() as Date,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id_pekerja)
        parcel.writeInt(id_perusahaan)
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

    companion object CREATOR : Parcelable.Creator<Dinas> {
        override fun createFromParcel(parcel: Parcel): Dinas {
            return Dinas(parcel)
        }

        override fun newArray(size: Int): Array<Dinas?> {
            return arrayOfNulls(size)
        }
    }
}