package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.navigation.FgTimeApp
import com.example.ui.theme.FgTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
            try {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:455868092391:android:dcd190d84dc93436a1b742")
                    .setApiKey("AIzaSyCGaaIQ5zP964WPLRxIjbl_axwZOFJZzm0")
                    .setProjectId("gen-lang-client-0188920817")
                    .setStorageBucket("gen-lang-client-0188920817.firebasestorage.app")
                    .setGcmSenderId("455868092391")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        enableEdgeToEdge()
        setContent {
            FgTimeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FgTimeApp()
                }
            }
        }
    }
}
