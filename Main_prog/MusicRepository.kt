package com.example.eeeeeee

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SunoRepository(private val apiKey: String) {
    private val client = OkHttpClient()

    // Generate music (start task)
    fun generate(prompt: String, callback: (String?) -> Unit) {
        val json = """
            {
                "title": "Generated Track",
                "prompt": "$prompt",
                "model_name": "chirp-v3-5",
                "model": "V4_5",
                "tags": "ambient, calm",
                "customMode": false, 
                "instrumental": true, 
                "callBackUrl": "https://suno-worker.jamesyeet808.workers.dev/callback"
            }
        """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.sunoapi.org/api/v1/generate") // NOTE: no callbackUrl here
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SunoRepository", "Generate failed: ${e.message}", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyString = it.body?.string()
                    Log.d("SunoRepository", "Generate response: $bodyString")

                    if (!it.isSuccessful) {
                        callback(null)
                        return
                    }

                    val jsonObj = JSONObject(bodyString ?: "{}")
                    val code = jsonObj.optInt("code", 400)

                    if (code == 200) {
                        val data = jsonObj.optJSONObject("data")
                        val taskId = data?.optString("task_id")
                        callback(taskId) // return taskId for polling
                    } else {
                        Log.e("SunoRepository", "Error: ${jsonObj.optString("msg")}")
                        callback(null)
                    }
                }
            }
        })
    }

    // Poll music status (check if complete)
    fun pollWorkerForAudio(taskId: String, callback: (String?) -> Unit) {
        val request = Request.Builder()
            .url("https://suno-worker.jamesyeet808.workers.dev/?taskId=$taskId") // correct Worker name
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SunoRepository", "Worker poll failed: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    Log.d("SunoRepository", "Worker poll response: $body")
                    if (it.isSuccessful && !body.isNullOrBlank()) {
                        try {
                            val json = JSONObject(body)
                            callback(json.optString("audio_url", null))
                        } catch (e: Exception) {
                            Log.e("SunoRepository", "Invalid JSON: $body")
                            callback(null)
                        }
                    } else {
                        callback(null)
                    }
                }
            }
        })
    }


}

