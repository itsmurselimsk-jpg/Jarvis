package com.example.data.device

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.SystemDiagnostics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DeviceTelemetryManager(private val context: Context) {

    private val _diagnostics = MutableStateFlow(SystemDiagnostics())
    val diagnostics: StateFlow<SystemDiagnostics> = _diagnostics.asStateFlow()

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var cameraId: String? = null

    init {
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
        } catch (_: Exception) {}
        registerBatteryReceiver()
        refreshAllTelemetry()
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let { updateBatteryFromIntent(it) }
            }
        }
        val stickyIntent = context.registerReceiver(receiver, filter)
        stickyIntent?.let { updateBatteryFromIntent(it) }
    }

    private fun updateBatteryFromIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 85

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 285)
        val tempC = tempTenths / 10.0f

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4100)
        val voltageV = voltageMv / 1000.0f

        val current = _diagnostics.value
        _diagnostics.value = current.copy(
            batteryPct = pct,
            isCharging = isCharging,
            batteryTempC = tempC,
            batteryVoltageV = voltageV
        )
    }

    fun refreshAllTelemetry() {
        // Memory Info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0)

        // Storage Info
        var totalStorageGb = 128f
        var usedStorageGb = 42f
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val totalBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize
            totalStorageGb = (totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)).toFloat()
            usedStorageGb = ((totalBytes - availableBytes).toDouble() / (1024.0 * 1024.0 * 1024.0)).toFloat()
        } catch (_: Exception) {}

        // Network Info
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connManager?.activeNetwork
        val caps = connManager?.getNetworkCapabilities(activeNetwork)
        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val netStatus = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "UPLINK: WI-FI SECURE"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "UPLINK: CELLULAR QUANTUM"
            isOnline -> "UPLINK: ACTIVE"
            else -> "UPLINK: DISCONNECTED / OFFLINE"
        }

        val cur = _diagnostics.value
        _diagnostics.value = cur.copy(
            usedRamMb = if (totalRamMb > 0) usedRamMb else cur.usedRamMb,
            totalRamMb = if (totalRamMb > 0) totalRamMb else cur.totalRamMb,
            usedStorageGb = usedStorageGb,
            totalStorageGb = totalStorageGb,
            networkStatus = netStatus,
            isOnline = isOnline
        )
    }

    fun toggleFlashlight(forceState: Boolean? = null): Boolean {
        val target = forceState ?: !_isFlashlightOn.value
        val camId = cameraId ?: return false
        return try {
            cameraManager?.setTorchMode(camId, target)
            _isFlashlightOn.value = target
            triggerHaptic(if (target) 40 else 20)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun triggerHaptic(durationMs: Long = 30) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
