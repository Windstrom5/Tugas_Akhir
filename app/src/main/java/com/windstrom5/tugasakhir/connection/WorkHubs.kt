package com.windstrom5.tugasakhir.connection

import android.app.Application
import android.util.Log
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions

class WorkHubs : Application() {
    companion object {
        lateinit var pusher: Pusher
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("WorkHubs", "Application onCreate called")

        val options = PusherOptions().apply {
            setCluster("ap1") // Replace with your Pusher cluster
        }

        pusher = Pusher("7d7b48e62cefacc3b046", options)
        pusher.connect()
    }
}