package com.example.data.model

import com.squareup.moshi.JsonClass

enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM_ALERT
}

data class JarvisMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val protocolTag: String? = null,
    val isTelemetryReport: Boolean = false
)

data class SystemDiagnostics(
    val batteryPct: Int = 100,
    val isCharging: Boolean = false,
    val batteryTempC: Float = 28.5f,
    val batteryVoltageV: Float = 4.1f,
    val usedRamMb: Long = 2048,
    val totalRamMb: Long = 6144,
    val usedStorageGb: Float = 32.0f,
    val totalStorageGb: Float = 128.0f,
    val networkStatus: String = "SECURE / LINKED",
    val isOnline: Boolean = true,
    val cpuFrequencyGhz: Float = 2.84f,
    val securityStatus: String = "DEFCON 5 - OPTIMAL"
) {
    val ramUsagePct: Float
        get() = if (totalRamMb > 0) (usedRamMb.toFloat() / totalRamMb.toFloat()) * 100f else 0f

    val storageUsagePct: Float
        get() = if (totalStorageGb > 0) (usedStorageGb / totalStorageGb) * 100f else 0f
}

data class StarkProtocol(
    val id: String,
    val name: String,
    val codeName: String,
    val description: String,
    val iconName: String,
    val isEmergency: Boolean = false,
    val isToggleable: Boolean = false,
    val isActive: Boolean = false
)

enum class JarvisTab {
    CORE_HUD,
    PROTOCOLS,
    TELEMETRY,
    ARCHIVES
}

// --- Gemini API Moshi DTOs ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = 0.7f,
    val maxOutputTokens: Int? = 800
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)
