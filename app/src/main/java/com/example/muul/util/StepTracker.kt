package com.example.muul.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.muul.data.local.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StepTracker(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private var steps = 0
    private var running = false
    private var routeId: String? = null
    private val repo = UserRepository(context)

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
            // TYPE_STEP_DETECTOR reports 1.0 for each step
            val delta = event.values?.getOrNull(0)?.toInt() ?: 0
            if (delta > 0) {
                steps += delta
                Log.d("MUUL_STEPS", "Paso detectado. Delta: $delta, Total: $steps")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start(routeId: String) {
        if (running) {
            Log.w("MUUL_SENSOR", "Intento de iniciar StepTracker cuando ya está corriendo")
            return
        }
        this.routeId = routeId
        steps = 0
        running = true
        
        if (stepDetector == null) {
            Log.e("MUUL_SENSOR", "No se puede iniciar: sensor no disponible")
            return
        }
        
        val registered = sensorManager.registerListener(listener, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
        Log.d("MUUL_SENSOR", "StepTracker iniciado para ruta: $routeId. Listener registrado: $registered")
    }

    fun stop() {
        if (!running) {
            Log.w("MUUL_SENSOR", "Intento de detener StepTracker cuando no está corriendo")
            return
        }
        running = false
        sensorManager.unregisterListener(listener)
        
        routeId?.let { rid ->
            val captured = steps
            Log.d("MUUL_SENSOR", "StepTracker detenido. Pasos capturados: $captured para ruta: $rid")
            
            // persist asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("MUUL_STEPS", "Guardando $captured pasos para ruta $rid")
                repo.addStepsForRoute(rid, captured)
                Log.d("MUUL_STEPS", "Pasos guardados exitosamente")
            }
        }
    }

    fun isRunning(): Boolean = running
}
