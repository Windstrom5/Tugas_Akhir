package com.windstrom5.tugasakhir.model

import android.os.Parcel
import android.os.Parcelable
import java.sql.Time
import java.util.Date

data class Lembur(
    val id: Int? = null,
    val id_perusahaan: Int,
    val id_pekerja: Int,
    val tanggal: Date, // Assuming you handle dates as strings
    val waktu_masuk: Time, // Assuming you handle times as strings
    val waktu_pulang: Time,
    val pekerjaan: String,
    val bukti: String,
    val status: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readSerializable() as Date,
        Time.valueOf(parcel.readString() ?: "00:00:00"),
        Time.valueOf(parcel.readString() ?: "00:00:00"),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id_pekerja)
        parcel.writeInt(id_perusahaan)
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

    companion object CREATOR : Parcelable.Creator<Lembur> {
        override fun createFromParcel(parcel: Parcel): Lembur {
            return Lembur(parcel)
        }

        override fun newArray(size: Int): Array<Lembur?> {
            return arrayOfNulls(size)
        }
    }
}
