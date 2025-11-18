package com.yourdomain.jarvis.ai

import android.content.Context
import com.yourdomain.jarvis.util.SecurePrefs
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class ChatGptApi(private val apiKey: String) {
    companion object {
        fun create(ctx: Context): ChatGptApi {
            val key = SecurePrefs.getApiKey(ctx) ?: throw Exception("OpenAI API key not set. Please enter it in app settings.")
            return ChatGptApi(key)
        }
    }

    private val client: OkHttpClient by lazy {
        val log = HttpLoggingInterceptor()
        log.level = HttpLoggingInterceptor.Level.BASIC
        OkHttpClient.Builder()
            .addInterceptor(log)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun sendPromptSync(prompt: String): String {
        val json = """
        {
          "model": "gpt-4o-mini",
          "messages": [{"role":"user","content":"${prompt.replace("\"","\\\"")}"}],
          "max_tokens": 200
        }
        """.trimIndent()

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val req = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}: ${resp.message}")
        val text = resp.body?.string() ?: ""
        // Minimal parsing: extract first content field if present
        val marker = "\"content\":\""
        val idx = text.indexOf(marker)
        return if (idx >= 0) {
            val sub = text.substring(idx + marker.length)
            val end = sub.indexOf('"')
            if (end >= 0) sub.substring(0, end).replace("\\n", "\n") else sub
        } else text
    }
}
