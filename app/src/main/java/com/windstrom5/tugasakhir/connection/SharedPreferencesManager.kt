package com.windstrom5.tugasakhir.connection

import android.content.Context
import android.content.SharedPreferences
import com.windstrom5.tugasakhir.model.Admin
import com.google.gson.Gson
import com.windstrom5.tugasakhir.model.Absen
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.Perusahaan

class SharedPreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    fun savePresensi(absen : Absen) {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val absenJson = gson.toJson(Absen)
        editor.putString("presensi", absenJson)
        editor.apply()
    }

    fun getPresensi(): Absen? {
        val gson = Gson()
        val absenJson = sharedPreferences.getString("presensi", null)
        return if (absenJson != null) {
            gson.fromJson(absenJson, Absen::class.java)
        } else {
            null
        }
    }
    fun removePresensi() {
        val editor = sharedPreferences.edit()
        editor.remove("presensi")
        editor.apply()
    }
    fun clearUserData() {
        val editor = sharedPreferences.edit()
        editor.remove("pekerja")
        editor.remove("admin")
        editor.remove("perusahaan")
        editor.remove("presensi")
        editor.apply()
    }

    fun saveAdmin(admin: Admin) {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val adminJson = gson.toJson(admin)
        editor.putString("admin", adminJson)
        editor.apply()
    }

    fun getAdmin(): Admin? {
        val gson = Gson()
        val adminJson = sharedPreferences.getString("admin", null)
        return if (adminJson != null) {
            gson.fromJson(adminJson, Admin::class.java)
        } else {
            null
        }
    }
    fun savePerusahaan(perusahaan: Perusahaan) {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val perusahaanJson = gson.toJson(perusahaan)
        editor.putString("perusahaan", perusahaanJson)
        editor.apply()
    }

    fun getPerusahaan(): Perusahaan? {
        val gson = Gson()
        val perusahaanJson = sharedPreferences.getString("perusahaan", null)
        return if (perusahaanJson != null) {
            gson.fromJson(perusahaanJson, Perusahaan::class.java)
        } else {
            null
        }
    }
    fun savePekerja(pekerja: Pekerja) {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val pekerjaJson = gson.toJson(pekerja)
        editor.putString("pekerja", pekerjaJson)
        editor.apply()
    }

    fun getPekerja(): Pekerja? {
        val gson = Gson()
        val adminJson = sharedPreferences.getString("pekerja", null)
        return if (adminJson != null) {
            gson.fromJson(adminJson, Pekerja::class.java)
        } else {
            null
        }
    }
}