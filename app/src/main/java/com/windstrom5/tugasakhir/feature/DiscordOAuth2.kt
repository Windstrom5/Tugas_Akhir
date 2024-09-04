package com.windstrom5.tugasakhir.feature

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.windstrom5.tugasakhir.BuildConfig
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.FormBody
import org.json.JSONException

data class DiscordUser(
    @SerializedName("id") val id: String,
    @SerializedName("banner") val banner: String,
    @SerializedName("global_name") val globalName: String,
    @SerializedName("avatar") val avatar: String,
)

// Your main DiscordOAuth2 class
class DiscordOAuth2 {
    private val clientId = BuildConfig.Discord_Client_Id
    private val clientSecret = BuildConfig.Discord_Client_Secret
    private val tokenUrl = "https://discord.com/api/oauth2/token"
    private val userUrl = "https://discord.com/api/users/@me"

    fun authenticate(callback: (DiscordUser?, String?) -> Unit) {
        val client = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("grant_type", "client_credentials")
            .add("scope", "identify") // Scope required to access user information
            .build()

        val request = Request.Builder()
            .url(tokenUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body()?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val jsonObject = JSONObject(responseBody)
                        val accessToken = jsonObject.getString("access_token")
                        getUserInfo(accessToken, callback)
                    } else {
                        callback(null, "Failed to authenticate: ${response.code()}")
                    }
                } catch (e: JSONException) {
                    callback(null, "Error parsing response: ${e.message}")
                }
            }
        })
    }
    fun fetchAvatar(avatarUrl: String, callback: (Bitmap?, String?) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(avatarUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val inputStream = response.body()?.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    callback(bitmap, null)
                } catch (e: Exception) {
                    callback(null, "Error parsing image: ${e.message}")
                }
            }
        })
    }
    private fun getUserInfo(accessToken: String, callback: (DiscordUser?, String?) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(userUrl)
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body()?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val gson = Gson()
                        Log.d("discord", responseBody)
                        val user = gson.fromJson(responseBody, DiscordUser::class.java)
                        callback(user, null)
                    } else {
                        callback(null, "Failed to fetch user info: ${response.code()}")
                    }
                } catch (e: JSONException) {
                    callback(null, "Error parsing response: ${e.message}")
                }
            }
        })
    }
}