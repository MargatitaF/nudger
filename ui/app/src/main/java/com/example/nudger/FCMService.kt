package com.example.nudger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService

class FCMService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Refreshed token: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch { // Launch on a background thread
            try {                
                // Placeholder for the actual server URL - will be addressed in a later step
                val baseUrl = applicationContext.getString(R.string.base_url)
                val url = URL("$baseUrl/register-token")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8") // Added charset
                connection.setRequestProperty("Accept", "application/json") // Added Accept header
                connection.doOutput = true

                val data = mapOf("token" to token)
                // Ensure you have the kotlinx.serialization.json.Json instance
                val jsonData = Json.encodeToString(data)

                connection.outputStream.use { os ->
                    os.write(jsonData.toByteArray(Charsets.UTF_8)) // Specified Charset
                }

                val responseCode = connection.responseCode
                Log.d("FCMService", "Server response code: $responseCode")
                // Optionally, read response from server if needed
                // connection.inputStream.bufferedReader().use { it.readText() }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e("FCMService", "Error sending token to server", e)
            }
        }
    }
}