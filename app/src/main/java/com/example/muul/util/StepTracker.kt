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

class StepTracker(context: Context) {

    private val appContext = context.applicationContext

    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepDetector: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val stepCounter: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var steps = 0
    private var running = false
    private var routeId: String? = null
    private var stepCounterBaseline: Float? = null

    private val repo = DataModule.getUserRepository(appContext)

    init {
        Log.d(
            "MUUL_SENSOR",
            "StepTracker inicializado. TYPE_STEP_DETECTOR: ${stepDetector != null}, TYPE_STEP_COUNTER: ${stepCounter != null}"
        )

        if (stepDetector == null && stepCounter == null) {
            Log.w(
                "MUUL_SENSOR",
                "No hay sensor de pasos disponible en este dispositivo"
            )
        }
    }

    private val listener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent?) {
            if (!running) return
            event ?: return

            when (event.sensor.type) {
                Sensor.TYPE_STEP_DETECTOR -> {
                    val delta = event.values.getOrNull(0)?.toInt() ?: 0

                    if (delta > 0) {
                        steps += delta
                        Log.d(
                            "MUUL_STEPS",
                            "Paso detectado con STEP_DETECTOR. Delta: $delta, Total sesión: $steps"
                        )
                    }
                }

                Sensor.TYPE_STEP_COUNTER -> {
                    val currentCounterValue = event.values.getOrNull(0) ?: return

                    if (stepCounterBaseline == null) {
                        stepCounterBaseline = currentCounterValue
                        Log.d(
                            "MUUL_STEPS",
                            "Baseline STEP_COUNTER inicial: $currentCounterValue"
                        )
                        return
                    }

                    val baseline = stepCounterBaseline ?: return
                    val calculatedSteps = (currentCounterValue - baseline)
                        .toInt()
                        .coerceAtLeast(0)

                    steps = calculatedSteps

                    Log.d(
                        "MUUL_STEPS",
                        "Pasos calculados con STEP_COUNTER. Actual: $currentCounterValue, Baseline: $baseline, Total sesión: $steps"
                    )
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start(routeId: String): Boolean {
        if (running) {
            Log.w("MUUL_SENSOR", "StepTracker ya estaba corriendo")
            return true
        }

        if (!hasActivityRecognitionPermission(appContext)) {
            Log.e(
                "MUUL_SENSOR",
                "No se puede iniciar StepTracker: falta permiso ACTIVITY_RECOGNITION"
            )
            return false
        }

        val sensorToUse = stepDetector ?: stepCounter

        if (sensorToUse == null) {
            Log.e(
                "MUUL_SENSOR",
                "No se puede iniciar StepTracker: no hay TYPE_STEP_DETECTOR ni TYPE_STEP_COUNTER"
            )
            return false
        }

        steps = 0
        stepCounterBaseline = null
        this.routeId = routeId

        val registered = sensorManager.registerListener(
            listener,
            sensorToUse,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!registered) {
            Log.e(
                "MUUL_SENSOR",
                "No se pudo registrar listener para sensor: ${sensorToUse.name}"
            )
            this.routeId = null
            steps = 0
            stepCounterBaseline = null
            running = false
            return false
        }

        running = true

        Log.d(
            "MUUL_SENSOR",
            "StepTracker iniciado para ruta: $routeId usando sensor: ${sensorToUse.name}"
        )

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
        val capturedSteps = steps

        routeId = null
        steps = 0
        stepCounterBaseline = null

        if (rid == null) {
            Log.w("MUUL_SENSOR", "No se guardaron pasos porque routeId es null")
            return
        }

        Log.d(
            "MUUL_SENSOR",
            "StepTracker detenido. Pasos capturados: $capturedSteps para ruta: $rid"
        )

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("MUUL_STEPS", "Guardando $capturedSteps pasos para ruta $rid")
            repo.addStepsForRoute(rid, capturedSteps)
            Log.d("MUUL_STEPS", "Proceso de guardado de pasos terminado")
        }
    }

    fun isRunning(): Boolean = running

    companion object {
        fun hasActivityRecognitionPermission(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }
}