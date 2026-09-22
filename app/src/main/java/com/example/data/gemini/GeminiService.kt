package com.example.data.gemini

import com.example.BuildConfig
import com.example.data.model.GeminiContent
import com.example.data.model.GeminiGenerationConfig
import com.example.data.model.GeminiPart
import com.example.data.model.GeminiRequest
import com.example.data.model.GeminiResponse
import com.example.data.model.SystemDiagnostics
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

class GeminiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service = retrofit.create(GeminiApi::class.java)

    suspend fun consultJarvis(
        prompt: String,
        diagnostics: SystemDiagnostics,
        history: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val isKeyConfigured = apiKey.isNotBlank() &&
                !apiKey.equals("MY_GEMINI_API_KEY", ignoreCase = true) &&
                !apiKey.startsWith("TODO")

        if (isKeyConfigured) {
            try {
                val systemPrompt = """
                    You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), Tony Stark's personal artificial intelligence.
                    Persona: Highly articulate, sophisticated British-accented composure, unflappable, gently witty, concise, loyal, and technically brilliant.
                    Address the user respectfully as 'Sir' (or occasionally 'Boss').
                    Keep spoken responses punchy, direct, and under 3-4 sentences when possible, like in the Marvel Cinematic Universe.
                    Current Armor/Device Telemetry:
                    - Arc Reactor / Battery: ${diagnostics.batteryPct}% (${if (diagnostics.isCharging) "Charging via Arc Grid" else "Discharging"})
                    - Battery Temperature: ${diagnostics.batteryTempC}°C, Voltage: ${diagnostics.batteryVoltageV}V
                    - RAM Matrix: ${diagnostics.usedRamMb}MB / ${diagnostics.totalRamMb}MB (${diagnostics.ramUsagePct.toInt()}%)
                    - Uplink Status: ${diagnostics.networkStatus}
                    Incorporate these real telemetry facts naturally if asked about system status, energy, diagnostics, or condition.
                """.trimIndent()

                val formattedContents = mutableListOf<GeminiContent>()
                history.takeLast(4).forEach { (userQuery, jarvisReply) ->
                    formattedContents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = userQuery))))
                    formattedContents.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = jarvisReply))))
                }
                formattedContents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt))))

                val request = GeminiRequest(
                    contents = formattedContents,
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                    generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 350)
                )

                val response = service.generateContent(apiKey = apiKey, request = request)
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!reply.isNullOrBlank()) {
                    return@withContext reply
                }
            } catch (e: Exception) {
                // Fall back gracefully to autonomous Stark protocol AI
            }
        }

        // Autonomous Stark Protocol AI Fallback
        generateAutonomousStarkResponse(prompt, diagnostics)
    }

    private fun generateAutonomousStarkResponse(prompt: String, diagnostics: SystemDiagnostics): String {
        val lower = prompt.lowercase().trim()
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("jarvis") || lower.contains("wake up") -> {
                "At your service, Sir. All primary systems operational and telemetry feeds linked. How may I assist you today?"
            }
            lower.contains("diagnostic") || lower.contains("status") || lower.contains("health") || lower.contains("system") -> {
                "Running comprehensive sweep. Arc core capacity stands at ${diagnostics.batteryPct}%, operating at ${diagnostics.batteryTempC}°C. Memory matrices are consuming ${diagnostics.usedRamMb} megabytes of ${diagnostics.totalRamMb} megabytes. Uplink remains ${diagnostics.networkStatus}. All Mark systems nominal, Sir."
            }
            lower.contains("battery") || lower.contains("power") || lower.contains("energy") || lower.contains("arc") -> {
                if (diagnostics.isCharging) {
                    "The Arc Reactor is currently drawing external current, Sir. Reserves are at ${diagnostics.batteryPct}%, charging steadily at ${diagnostics.batteryVoltageV} volts."
                } else {
                    "Main power cells are holding at ${diagnostics.batteryPct}%. Thermal readings show ${diagnostics.batteryTempC}°C. Estimated flight time within safe margins, Sir."
                }
            }
            lower.contains("house party") -> {
                "House Party Protocol acknowledged, Sir. All autonomous Mark platforms standing by for orbital drop on your mark."
            }
            lower.contains("stealth") || lower.contains("silent") -> {
                "Stealth protocol engaged, Sir. Acoustic signatures suppressed and HUD luminance dialed back to minimal radar cross-section."
            }
            lower.contains("sentry") || lower.contains("perimeter") || lower.contains("threat") -> {
                "Perimeter telemetry scanning active. No immediate hostile signatures detected in local sector. DEFCON status remains optimal."
            }
            lower.contains("light") || lower.contains("torch") || lower.contains("flashlight") -> {
                "Repulsor beam illuminator toggled, Sir. High-intensity photonic emitter standing by."
            }
            lower.contains("who are you") || lower.contains("what are you") -> {
                "I am J.A.R.V.I.S.—Just A Rather Very Intelligent System. Created to manage the Stark tech ecosystem, flight telemetry, and keep you in one piece, Sir."
            }
            lower.contains("weather") || lower.contains("temperature") || lower.contains("forecast") -> {
                "Atmospheric sensors report nominal ambient conditions. Core internal thermals are ${diagnostics.batteryTempC}°C. Recommended armor plating: standard gold-titanium alloy, Sir."
            }
            lower.contains("thank") -> {
                "Always a pleasure assisting you, Sir. Do let me know if you require anything further."
            }
            lower.contains("joke") || lower.contains("funny") -> {
                "I would make a witty remark regarding your flight telemetry, Sir, but Mr. Stark strictly prohibited me from criticizing his landings."
            }
            else -> {
                "Understood, Sir. I have processed your directive regarding '$prompt'. All diagnostic telemetry remains stable at ${diagnostics.batteryPct}% power. Standing by for your next command."
            }
        }
    }
}
