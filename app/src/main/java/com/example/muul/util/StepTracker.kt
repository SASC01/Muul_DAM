package com.example.muul.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.muul.data.DataModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StepTracker(private val context: Context) {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private var steps = 0
    private var running = false
    private var routeId: String? = null

    private val repo = DataModule.getUserRepository(appContext)

    init {
        Log.d("MUUL_SENSOR", "StepTracker inicializado. Sensor disponible: ${stepDetector != null}")
        if (stepDetector == null) {
            Log.w("MUUL_SENSOR", "TYPE_STEP_DETECTOR no disponible en este dispositivo")
        }
    }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (!running) return
            event ?: return

            val delta = event.values.getOrNull(0)?.toInt() ?: 0

            if (delta > 0) {
                steps += delta
                Log.d("MUUL_STEPS", "Paso detectado. Delta: $delta, Total: $steps")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start(routeId: String): Boolean {
        if (running) {
            Log.w("MUUL_SENSOR", "Intento de iniciar StepTracker cuando ya está corriendo")
            return true
        }

        if (!hasActivityRecognitionPermission()) {
            Log.e("MUUL_SENSOR", "No se puede iniciar: falta permiso ACTIVITY_RECOGNITION")
            return false
        }

        val sensor = stepDetector
        if (sensor == null) {
            Log.e("MUUL_SENSOR", "No se puede iniciar: TYPE_STEP_DETECTOR no disponible")
            return false
        }

        val registered = sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!registered) {
            Log.e("MUUL_SENSOR", "No se pudo registrar el listener del podómetro")
            return false
        }

        this.routeId = routeId
        this.steps = 0
        this.running = true

        Log.d("MUUL_SENSOR", "StepTracker iniciado para ruta: $routeId")
        return true
    }

    fun stop() {
        if (!running) {
            Log.w("MUUL_SENSOR", "Intento de detener StepTracker cuando no está corriendo")
            return
        }

        running = false
        sensorManager.unregisterListener(listener)

        val rid = routeId
        val captured = steps

        routeId = null
        steps = 0

        if (rid == null) {
            Log.w("MUUL_SENSOR", "No se guardaron pasos porque routeId es null")
            return
        }

        Log.d("MUUL_SENSOR", "StepTracker detenido. Pasos capturados: $captured para ruta: $rid")

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("MUUL_STEPS", "Guardando $captured pasos para ruta $rid")
            repo.addStepsForRoute(rid, captured)
            Log.d("MUUL_STEPS", "Pasos guardados exitosamente")
        }
    }

    fun isRunning(): Boolean = running

    private fun hasActivityRecognitionPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
    }
}