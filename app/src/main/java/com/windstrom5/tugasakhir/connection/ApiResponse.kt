package com.windstrom5.tugasakhir.connection

import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.UpdatedPerusahaan

data class ApiResponse(
    val status: String,
    val message: String,
    val profile_path: String,
    val id:Int,
    val admin: Admin? = null,
    val pekerja: Pekerja? = null,
    val perusahaan: UpdatedPerusahaan? =null
)
