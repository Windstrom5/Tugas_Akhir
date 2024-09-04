package com.windstrom5.tugasakhir.model

import java.util.Date

class login_session (
    val id_user: Int,
    val token: String,
    var create_at: Date,
    val role: String
)