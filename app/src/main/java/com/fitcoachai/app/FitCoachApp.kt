package com.fitcoachai.app

import android.app.Application
import org.maplibre.android.MapLibre

class FitCoachApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialisation de MapLibre (Anciennement Mapbox)
        MapLibre.getInstance(this)
    }
}
