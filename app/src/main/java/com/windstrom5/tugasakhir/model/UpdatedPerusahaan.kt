package com.windstrom5.tugasakhir.model

import java.sql.Date
import java.sql.Time

data class UpdatedPerusahaan (
    val id: Int? = null,
    val nama: String,
    val latitude: Double,
    val longitude: Double,
    val jam_masuk: String,
    val jam_keluar: String,
    val batas_aktif: String,
    val logo: String?,
    val secret_key: String,
    val holiday: String
)