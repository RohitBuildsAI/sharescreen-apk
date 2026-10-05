package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class CompanionApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
    }

    private fun initializeFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(getString(R.string.google_app_id))
                    .setApiKey(getString(R.string.google_api_key))
                    .setProjectId(getString(R.string.project_id))
                    .setGcmSenderId(getString(R.string.gcm_defaultSenderId))
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("CompanionApplication", "FirebaseApp initialized with explicit options.")
            } catch (e: Exception) {
                Log.w("CompanionApplication", "Failed to initialize Firebase with explicit options, attempting fallback", e)
                try {
                    FirebaseApp.initializeApp(this)
                } catch (fallbackError: Exception) {
                    Log.e("CompanionApplication", "FirebaseApp fallback initialization failed", fallbackError)
                }
            }
        }
    }
}
