package com.windstrom5.tugasakhir.connection

import android.util.Log
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONException
import org.json.JSONObject
import okhttp3.Response

class WebSocketPerusahaan: WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        Log.d("WebSocket", "WebSocket connection opened")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        // Handle incoming message from WebSocket
        Log.d("WebSocket", "Received message: $text")
        // Parse the received JSON string
        try {
            val jsonObject = JSONObject(text)
            val pekerjaData = jsonObject.getJSONObject("pekerjadata")
            val role = jsonObject.getString("role")
            // Handle the received data as needed
            // For example, update UI with the received data
        } catch (e: JSONException) {
            Log.e("WebSocket", "Error parsing JSON: ${e.message}")
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        Log.d("WebSocket", "WebSocket connection closed")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        // Handle WebSocket connection failure
        Log.e("WebSocket", "WebSocket connection failure: ${t.message}")
    }
}