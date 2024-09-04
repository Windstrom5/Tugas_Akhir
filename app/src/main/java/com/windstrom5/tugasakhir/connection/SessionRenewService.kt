package com.windstrom5.tugasakhir.connection

import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.windstrom5.tugasakhir.activity.AdminActivity
import com.windstrom5.tugasakhir.activity.LoginActivity
import com.windstrom5.tugasakhir.activity.UserActivity
import com.windstrom5.tugasakhir.model.login_session
import com.google.gson.Gson
import java.util.Date

class SessionRenewService: IntentService("SessionRenewService")  {
    override fun onHandleIntent(intent: Intent?) {
        val session = getSession()
        if (session != null) {
            if (isSessionExpired(session)) {
                redirectToLogin()
            } else {
                renewSession(session)
                redirectToRoleActivity(session.role)
            }
        }
    }

    private fun isSessionExpired(session: login_session): Boolean {
        val currentTime = System.currentTimeMillis()
        val sessionCreateTime = session.create_at.time
        val sessionDurationMillis = 8 * 60 * 60 * 1000 // 8 hours in milliseconds
        return currentTime - sessionCreateTime >= sessionDurationMillis
    }

    private fun redirectToLogin() {
        // Session expired, redirect to login page (MainActivity)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun redirectToRoleActivity(role: String) {
        // Session still valid, redirect based on the user's role
        val targetActivity = when (role) {
            "Admin" -> AdminActivity::class.java
            else -> UserActivity::class.java // Default to login page for unknown roles
        }

        val intent = Intent(this, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun renewSession(session: login_session) {
        Log.d("SessionRenewService", "Renewing session...")
        // For testing purposes, we'll update the 'create_at' field to the current time
        session.create_at = Date()

        // Save the updated session to SharedPreferences
        saveSession(session)

        Log.d("SessionRenewService", "Session renewed successfully.")
    }

    private fun saveSession(login_session: login_session) {
        val editor = getSharedPreferences("session", Context.MODE_PRIVATE).edit()
        val gson = Gson()
        val loginJson = gson.toJson(login_session)
        editor.putString("login", loginJson)
        editor.apply()
    }

    private fun getSession(): login_session? {
        val gson = Gson()
        val preferences = getSharedPreferences("session", Context.MODE_PRIVATE)
        val loginJson = preferences.getString("login", null)
        return if (loginJson != null) {
            gson.fromJson(loginJson, login_session::class.java)
        } else {
            null
        }
    }
//    companion object {
//        fun setServiceAlarm(context: Context, intervalMillis: Long) {
//            val intent = Intent(context, SessionRenewService::class.java)
//            val pendingIntent = PendingIntent.getService(
//                context,
//                0,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT  // Add mutability flag
//            )
//
//            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            alarmManager.setInexactRepeating(
//                AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                SystemClock.elapsedRealtime() + intervalMillis,
//                intervalMillis,
//                pendingIntent
//            )
//        }
//
//        fun cancelServiceAlarm(context: Context) {
//            val intent = Intent(context, SessionRenewService::class.java)
//            val pendingIntent = PendingIntent.getService(
//                context,
//                0,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT
//            )
//
//            pendingIntent?.let {
//                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//                alarmManager.cancel(it)
//                it.cancel()
//            }
//        }
//    }
}