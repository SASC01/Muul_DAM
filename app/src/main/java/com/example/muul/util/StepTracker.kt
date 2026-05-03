package com.example.muul.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (!running) return
            event ?: return
            // TYPE_STEP_DETECTOR reports 1.0 for each step
            val delta = event.values?.getOrNull(0)?.toInt() ?: 0
            steps += delta
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start(routeId: String) {
        if (running) return
        this.routeId = routeId
        steps = 0
        running = true
        stepDetector?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager.unregisterListener(listener)
        routeId?.let { rid ->
            val captured = steps
            // persist asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                repo.addStepsForRoute(rid, captured)
            }
        }
    }

    fun isRunning(): Boolean = running
}
