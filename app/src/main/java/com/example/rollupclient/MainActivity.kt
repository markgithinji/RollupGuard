package com.example.rollupclient

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "RollupClient"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "🚀 App starting...")

        setContent {
            MaterialTheme {
                RollupDashboard()
            }
        }
    }
}