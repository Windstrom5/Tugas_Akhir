package com.windstrom5.tugasakhir.model

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

data class Izin(
    val id: Int? = null,
    val id_pekerja: Int,
    val id_perusahaan: Int,
    val tanggal: Date,
    val kategori: String,
    val alasan: String,
    val bukti: String,
    val status: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readSerializable() as Date,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id_pekerja)
        parcel.writeInt(id_perusahaan)
        parcel.writeLong(tanggal.time)
        parcel.writeString(kategori)
        parcel.writeString(alasan)
        parcel.writeString(bukti)
        parcel.writeString(bukti)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Izin> {
        override fun createFromParcel(parcel: Parcel): Izin {
            return Izin(parcel)
        }

        override fun newArray(size: Int): Array<Izin?> {
            return arrayOfNulls(size)
        }
    }
}
