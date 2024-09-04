package com.windstrom5.tugasakhir.connection

import org.json.JSONObject

interface WebSocketPekerjaListenerCallback {
    fun onPekerjaDataReceived(pekerjaData: JSONObject, role: String)
}