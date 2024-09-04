package com.windstrom5.tugasakhir.connection

//import android.util.Log
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//
//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        super.onMessageReceived(remoteMessage)
//
//        // Handle the incoming FCM message
//        Log.d(TAG, "From: ${remoteMessage.from}")
//
//        // Check if the message contains data
//        if (remoteMessage.data.isNotEmpty()) {
//            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
//            // Handle the data payload
//        }
//
//        // Check if the message contains a notification payload
//        remoteMessage.notification?.let {
//            Log.d(TAG, "Message Notification Body: ${it.body}")
//            // Handle the notification payload
//        }
//    }
//
//    companion object {
//        private const val TAG = "MyFirebaseMsgService"
//    }
//}
