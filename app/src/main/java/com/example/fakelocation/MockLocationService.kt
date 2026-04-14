package com.example.fakelocation

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MockLocationService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Votre logique de fausse localisation ira ici
        return START_STICKY
    }
}