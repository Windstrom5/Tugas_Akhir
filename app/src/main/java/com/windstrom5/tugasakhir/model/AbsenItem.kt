package com.windstrom5.tugasakhir.model

import java.sql.Time
import java.util.Date

data class AbsenItem(
    val id: Int? = null,
    val id_perusahaan: Int?= null,
    val id_pekerja: Int?=null,
    val nama_pekerja: String,
    val nama_perusahaan: String,
    val tanggal: Date,
    val masuk: Time,
    val keluar: Time? = null
)