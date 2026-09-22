package com.example.data.device

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsEngine = tts ?: return
            // Prefer UK English for authentic JARVIS accent, fallback to US / default
            val result = ttsEngine.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEngine.setLanguage(Locale.US)
            }
            ttsEngine.setPitch(0.96f)
            ttsEngine.setSpeechRate(1.04f)

            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
            _isReady.value = true
        }
    }

    fun toggleVoiceOutput(enable: Boolean? = null) {
        val next = enable ?: !_isEnabled.value
        _isEnabled.value = next
        if (!next) {
            stop()
        }
    }

    fun speak(text: String) {
        if (!_isEnabled.value || !_isReady.value) return
        val ttsEngine = tts ?: return
        try {
            // Clean any markdown formatting (asterisks, hashtags) for clean speech
            val cleanText = text
                .replace(Regex("[*#_`~]"), "")
                .replace(Regex("\\(.*?\\)"), "")
                .trim()
            if (cleanText.isNotBlank()) {
                val utteranceId = UUID.randomUUID().toString()
                val params = Bundle()
                ttsEngine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            _isSpeaking.value = false
            _isReady.value = false
        } catch (_: Exception) {}
    }
}
