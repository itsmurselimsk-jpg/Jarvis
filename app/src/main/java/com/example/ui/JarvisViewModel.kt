package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.device.DeviceTelemetryManager
import com.example.data.device.TtsManager
import com.example.data.gemini.GeminiRepository
import com.example.data.model.JarvisMessage
import com.example.data.model.JarvisTab
import com.example.data.model.MessageSender
import com.example.data.model.StarkProtocol
import com.example.data.model.SystemDiagnostics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val deviceManager = DeviceTelemetryManager(application)
    private val ttsManager = TtsManager(application)
    private val geminiRepo = GeminiRepository()

    val diagnostics: StateFlow<SystemDiagnostics> = deviceManager.diagnostics
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val isFlashlightOn: StateFlow<Boolean> = deviceManager.isFlashlightOn
    val voiceEnabled: StateFlow<Boolean> = ttsManager.isEnabled

    private val _selectedTab = MutableStateFlow(JarvisTab.CORE_HUD)
    val selectedTab: StateFlow<JarvisTab> = _selectedTab.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isOverclocked = MutableStateFlow(false)
    val isOverclocked: StateFlow<Boolean> = _isOverclocked.asStateFlow()

    private val _protocols = MutableStateFlow(initialProtocols())
    val protocols: StateFlow<List<StarkProtocol>> = _protocols.asStateFlow()

    private val _messages = MutableStateFlow<List<JarvisMessage>>(emptyList())
    val messages: StateFlow<List<JarvisMessage>> = _messages.asStateFlow()

    init {
        // Initial Jarvis greeting
        val initialGreeting = JarvisMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.JARVIS,
            text = "Good day, Sir. J.A.R.V.I.S. online and calibrated to your biometric signature. Arc reactor telemetry nominal. All protocols standing by.",
            protocolTag = "INIT"
        )
        _messages.value = listOf(initialGreeting)
        // Speak initial greeting once TTS is ready
        viewModelScope.launch {
            delay(1200)
            ttsManager.speak("Good day, Sir. J.A.R.V.I.S. online. All systems nominal.")
        }
    }

    fun selectTab(tab: JarvisTab) {
        deviceManager.triggerHaptic(20)
        _selectedTab.value = tab
    }

    fun toggleVoice() {
        deviceManager.triggerHaptic(25)
        ttsManager.toggleVoiceOutput()
    }

    fun toggleFlashlight() {
        val turnedOn = deviceManager.toggleFlashlight()
        val newState = deviceManager.isFlashlightOn.value
        val msg = if (newState) {
            "Illumination beam activated, Sir."
        } else {
            "Illumination offline."
        }
        addSystemLog(msg, protocolTag = "BEAM")
        if (voiceEnabled.value) {
            ttsManager.speak(msg)
        }
    }

    fun sendUserPrompt(userText: String) {
        if (userText.isBlank()) return
        val trimmed = userText.trim()
        deviceManager.triggerHaptic(30)

        val userMsg = JarvisMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = trimmed
        )
        _messages.value = _messages.value + userMsg

        _isProcessing.value = true

        viewModelScope.launch {
            try {
                // Build short history
                val history = _messages.value.takeLast(6).mapNotNull {
                    if (it.sender == MessageSender.USER) it.text to "" else null
                }

                val reply = geminiRepo.consultJarvis(
                    prompt = trimmed,
                    diagnostics = diagnostics.value,
                    history = history
                )

                _isProcessing.value = false

                val jarvisMsg = JarvisMessage(
                    id = UUID.randomUUID().toString(),
                    sender = MessageSender.JARVIS,
                    text = reply
                )
                _messages.value = _messages.value + jarvisMsg

                // Vocalize response
                ttsManager.speak(reply)
            } catch (e: Exception) {
                _isProcessing.value = false
                val fallbackReply = "Telemetry uplink disrupted, Sir. However, local processors confirm core stability at ${diagnostics.value.batteryPct}% power."
                _messages.value = _messages.value + JarvisMessage(
                    id = UUID.randomUUID().toString(),
                    sender = MessageSender.JARVIS,
                    text = fallbackReply
                )
                ttsManager.speak(fallbackReply)
            }
        }
    }

    fun executeProtocol(protocol: StarkProtocol) {
        deviceManager.triggerHaptic(45)

        val currentList = _protocols.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == protocol.id }
        if (index != -1) {
            val updated = currentList[index].copy(isActive = !currentList[index].isActive)
            currentList[index] = updated
            _protocols.value = currentList

            when (protocol.id) {
                "proto_diag" -> runFullDiagnosticScan()
                "proto_overclock" -> {
                    _isOverclocked.value = updated.isActive
                    val text = if (updated.isActive) {
                        "Overclock protocol engaged. Core throttling bypassed. HUD telemetry in high-intensity spectrum."
                    } else {
                        "Overclock disengaged. Returning to standard energy conservation mode."
                    }
                    addJarvisMessage(text, "OVERCLOCK")
                    ttsManager.speak(text)
                }
                "proto_flashlight" -> {
                    deviceManager.toggleFlashlight(updated.isActive)
                    val text = if (updated.isActive) "Photon repulsor beam ignited." else "Photon emitter inactive."
                    addJarvisMessage(text, "LIGHT")
                    ttsManager.speak(text)
                }
                "proto_stealth" -> {
                    val text = if (updated.isActive) {
                        "Stealth protocol engaged. External audio signatures suppressed, Sir."
                    } else {
                        "Stealth protocol cancelled. Normal radar signature restored."
                    }
                    addJarvisMessage(text, "STEALTH")
                    ttsManager.speak(text)
                }
                "proto_house_party" -> {
                    val text = "House Party Protocol initialized, Sir! Orbital drop trajectory computed for Mark suits XL through LXXXV."
                    addJarvisMessage(text, "HOUSE_PARTY")
                    ttsManager.speak(text)
                }
                "proto_sentry" -> {
                    val text = if (updated.isActive) {
                        "Sentry mode armed. Perimeter sensor sweep initialized. DEFCON 3."
                    } else {
                        "Sentry mode disengaged. All zones clear."
                    }
                    addJarvisMessage(text, "SENTRY")
                    ttsManager.speak(text)
                }
            }
        }
    }

    fun runFullDiagnosticScan() {
        deviceManager.triggerHaptic(50)
        deviceManager.refreshAllTelemetry()
        val d = diagnostics.value
        val report = """
            FULL DIAGNOSTIC REPORT:
            • Core Power: ${d.batteryPct}% (${if (d.isCharging) "Charging" else "Discharging"})
            • Thermal Matrix: ${d.batteryTempC}°C
            • Cell Voltage: ${d.batteryVoltageV}V
            • RAM Allocated: ${d.usedRamMb}MB / ${d.totalRamMb}MB (${d.ramUsagePct.toInt()}%)
            • Storage Matrix: ${String.format("%.1f", d.usedStorageGb)}GB / ${String.format("%.1f", d.totalStorageGb)}GB
            • Telemetry Link: ${d.networkStatus}
            • Armor Readiness: 100% OPTIMAL
        """.trimIndent()

        addJarvisMessage(report, protocolTag = "DIAGNOSTICS", isTelemetry = true)
        ttsManager.speak("Diagnostic sweep complete, Sir. Arc reactor at ${d.batteryPct}%, thermals at ${d.batteryTempC} degrees. All systems are fully operational.")
    }

    fun vocalizeMessage(message: JarvisMessage) {
        deviceManager.triggerHaptic(20)
        if (ttsManager.isSpeaking.value) {
            ttsManager.stop()
        } else {
            ttsManager.speak(message.text)
        }
    }

    fun clearHistory() {
        deviceManager.triggerHaptic(30)
        _messages.value = listOf(
            JarvisMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.JARVIS,
                text = "Mission logs cleared, Sir. Fresh telemetry session initiated.",
                protocolTag = "CLEARED"
            )
        )
    }

    fun setListening(listening: Boolean) {
        _isListening.value = listening
        if (listening) {
            deviceManager.triggerHaptic(40)
        }
    }

    private fun addJarvisMessage(text: String, protocolTag: String? = null, isTelemetry: Boolean = false) {
        val msg = JarvisMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.JARVIS,
            text = text,
            protocolTag = protocolTag,
            isTelemetryReport = isTelemetry
        )
        _messages.value = _messages.value + msg
    }

    private fun addSystemLog(text: String, protocolTag: String? = null) {
        val msg = JarvisMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.SYSTEM_ALERT,
            text = text,
            protocolTag = protocolTag
        )
        _messages.value = _messages.value + msg
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    private fun initialProtocols(): List<StarkProtocol> = listOf(
        StarkProtocol(
            id = "proto_diag",
            name = "Diagnostic Sweep",
            codeName = "PROTOCOL 01",
            description = "Performs full hardware scan across power cells, memory, and uplink sensors.",
            iconName = "diagnostic"
        ),
        StarkProtocol(
            id = "proto_flashlight",
            name = "Repulsor Illuminator",
            codeName = "PROTOCOL 02",
            description = "Engages device photonic emitter / torch beam.",
            iconName = "light",
            isToggleable = true
        ),
        StarkProtocol(
            id = "proto_overclock",
            name = "Overclock Core",
            codeName = "PROTOCOL 03",
            description = "Bypasses safety governor for high-intensity visual & computation feedback.",
            iconName = "overclock",
            isToggleable = true
        ),
        StarkProtocol(
            id = "proto_stealth",
            name = "Stealth Envelope",
            codeName = "PROTOCOL 04",
            description = "Mutes acoustic telemetry and suppresses radar cross-section.",
            iconName = "stealth",
            isToggleable = true
        ),
        StarkProtocol(
            id = "proto_house_party",
            name = "House Party",
            codeName = "PROTOCOL 05",
            description = "Deploys all autonomous Mark platforms for perimeter supremacy.",
            iconName = "party",
            isEmergency = true
        ),
        StarkProtocol(
            id = "proto_sentry",
            name = "Perimeter Sentry",
            codeName = "PROTOCOL 06",
            description = "Activates passive threat detection radar in local sector.",
            iconName = "sentry",
            isToggleable = true
        )
    )
}
