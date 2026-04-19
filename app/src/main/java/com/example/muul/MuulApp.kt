package com.example.muul

import android.app.Application
import com.mapbox.common.MapboxOptions

class MuulApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    }
}