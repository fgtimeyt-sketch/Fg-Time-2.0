package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class FgTimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                Log.d("FgTimeApp", "FirebaseApp already initialized")
                return
            }

            // Attempt standard resource-based initialization first
            val app = FirebaseApp.initializeApp(this)
            if (app != null) {
                Log.d("FgTimeApp", "FirebaseApp initialized via standard resources")
                return
            }
        } catch (e: Exception) {
            Log.w("FgTimeApp", "Standard Firebase initialization encountered: ${e.message}")
        }

        // Explicit fallback using configured google-services credentials
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:455868092391:android:dcd190d84dc93436a1b742")
                .setApiKey("AIzaSyCGaaIQ5zP964WPLRxIjbl_axwZOFJZzm0")
                .setProjectId("gen-lang-client-0188920817")
                .setStorageBucket("gen-lang-client-0188920817.firebasestorage.app")
                .setGcmSenderId("455868092391")
                .build()

            FirebaseApp.initializeApp(this, options)
            Log.i("FgTimeApp", "FirebaseApp successfully initialized with explicit FirebaseOptions")
        } catch (e: Exception) {
            Log.e("FgTimeApp", "Failed to initialize FirebaseApp", e)
        }
    }
}
