package com.example.muul.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        if (!hasPermission()) return null

        return suspendCancellableCoroutine { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(Pair(location.latitude, location.longitude))
                    } else {
                        // Pedir ubicación fresca
                        requestFreshLocation { freshLocation ->
                            if (cont.isActive) {
                                cont.resume(freshLocation)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(callback: (Pair<Double, Double>?) -> Unit) {
        if (!hasPermission()) {
            callback(null)
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 3000L
        ).setMaxUpdates(1)
            .setMinUpdateIntervalMillis(1000L)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    callback(Pair(loc.latitude, loc.longitude))
                } else {
                    callback(null)
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request, locationCallback, Looper.getMainLooper()
        )
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Pair<Double, Double>> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000L
        ).setMinUpdateIntervalMillis(5000L).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(Pair(location.latitude, location.longitude))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request, callback, Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}