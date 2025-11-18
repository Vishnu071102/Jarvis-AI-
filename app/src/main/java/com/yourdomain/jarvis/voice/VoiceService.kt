package com.yourdomain.jarvis.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.yourdomain.jarvis.MainActivity
import com.yourdomain.jarvis.R
import com.yourdomain.jarvis.ai.ChatGptApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class VoiceService : Service(), RecognitionListener {
    private lateinit var speech: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var tts: TextToSpeech
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())

        speech = SpeechRecognizer.createSpeechRecognizer(this)
        speech.setRecognitionListener(this)

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts.language = Locale.getDefault()
        }

        startListening()
    }

    private fun startListening() {
        try {
            speech.startListening(recognizerIntent)
        } catch (e: Exception) {
            Handler(mainLooper).postDelayed({ startListening() }, 500)
        }
    }

    private fun stopListening() {
        try { speech.stopListening() } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        speech.destroy()
        tts.shutdown()
        super.onDestroy()
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        Handler(mainLooper).postDelayed({ startListening() }, 300)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) handleVoiceText(matches[0])
        Handler(mainLooper).postDelayed({ startListening() }, 300)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0].lowercase(Locale.getDefault())
            if (text.contains("hey jarvis") || text.contains("jarvis")) {
                stopListening()
                Handler(mainLooper).postDelayed({ captureCommandAndProcess() }, 250)
            }
        }
    }

    private fun captureCommandAndProcess() {
        val cmdIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Jarvis: Listening for command")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        val singleListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { startListening() }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0]
                    scope.launch { processCommand(command) }
                }
                startListening()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        val tempSpeech = SpeechRecognizer.createSpeechRecognizer(this)
        tempSpeech.setRecognitionListener(singleListener)
        tempSpeech.startListening(cmdIntent)
    }

    private suspend fun processCommand(command: String) {
        val api = ChatGptApi.create(applicationContext)
        val prompt = "You are Jarvis, a concise helpful Android assistant. The user said: \"$command\". Reply with a human-friendly response. If an action should be executed, include a line formatted as ACTION:<TYPE>|<PAYLOAD> e.g. ACTION:OPEN_APP|com.whatsapp"
        try {
            val resp = api.sendPromptSync(prompt)
            val text = resp
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_resp")
            parseAndExecuteActions(text)
        } catch (e: Exception) {
            tts.speak("Sorry, I couldn't reach the AI service.", TextToSpeech.QUEUE_FLUSH, null, "jarvis_err")
        }
    }

    private fun parseAndExecuteActions(text: String) {
        val lines = text.split('\n')
        for (l in lines) {
            val s = l.trim()
            if (s.startsWith("ACTION:")) {
                val payload = s.removePrefix("ACTION:")
                val parts = payload.split("|")
                when(parts[0]) {
                    "OPEN_APP" -> {
                        val pkg = parts.getOrNull(1) ?: continue
                        val pm = packageManager.getLaunchIntentForPackage(pkg)
                        pm?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                    // More actions can be added here
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        val nIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, nIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "jarvis_channel")
            .setContentTitle("Jarvis is listening")
            .setContentText("Say 'Hey Jarvis' to start")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel("jarvis_channel", "Jarvis Listener", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(chan)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onRmsChanged(rmsdB: Float) {}
}
