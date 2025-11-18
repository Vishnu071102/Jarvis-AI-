package com.yourdomain.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yourdomain.jarvis.util.SecurePrefs

class MainActivity : AppCompatActivity() {
    private val RECORD_AUDIO_REQ = 101
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnable = findViewById<Button>(R.id.btnEnable)
        val btnStart = findViewById<Button>(R.id.btnStartService)
        val btnStop = findViewById<Button>(R.id.btnStopService)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        btnEnable.setOnClickListener {
            requestPermissionsIfNeeded()
        }

        btnStart.setOnClickListener {
            startService(Intent(this, com.yourdomain.jarvis.voice.VoiceService::class.java))
            tvStatus.text = "Status: Service started"
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, com.yourdomain.jarvis.voice.VoiceService::class.java))
            tvStatus.text = "Status: Service stopped"
        }

        // First-run API key prompt
        val existing = SecurePrefs.getApiKey(this)
        if (existing.isNullOrEmpty()) {
            val input = android.widget.EditText(this)
            AlertDialog.Builder(this)
                .setTitle("OpenAI API Key")
                .setMessage("Enter your OpenAI API key (stored only on this device)")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val key = input.text.toString().trim()
                    if (key.isNotEmpty()) {
                        SecurePrefs.saveApiKey(this, key)
                        Toast.makeText(this, "API key saved securely", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // simple permission state show
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            tvStatus.text = "Status: Microphone permission not granted"
        } else tvStatus.text = "Status: Ready"
    }

    private fun requestPermissionsIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQ)
        } else {
            startService(Intent(this, com.yourdomain.jarvis.voice.VoiceService::class.java))
        }
    }
}
