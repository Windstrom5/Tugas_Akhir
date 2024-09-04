package com.windstrom5.tugasakhir.model

import android.os.Parcel
import android.os.Parcelable
import java.sql.Time
import java.util.Date

data class session_lembur(
    val id: Int? = null,
    val id_lembur: Int,
    val jam: Date,
    val keterangan: String,
    val bukti: String,
    val status: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readInt(),
        Date(parcel.readLong()), // Read Date as long timestamp
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeInt(id_lembur)
        parcel.writeLong(jam.time) // Write Date as long timestamp
        parcel.writeString(keterangan)
        parcel.writeString(bukti)
        parcel.writeString(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<session_lembur> {
        override fun createFromParcel(parcel: Parcel): session_lembur {
            return session_lembur(parcel)
        }

        override fun newArray(size: Int): Array<session_lembur?> {
            return arrayOfNulls(size)
        }
    }
}
