package com.example.nudger

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

import android.content.Context
import com.example.nudger.R

class ApiService(private val context: Context) {
    private val client = OkHttpClient()
    private val baseUrl = context.getString(R.string.base_url)
      data class ScheduleRequest(
        val token: String,
        val title: String,
        val time: String,
        val frequency: String,
        val weekday: Int? = null,
        val monthday: Int? = null,
        val enddate: String? = null
    )
    
    data class NotificationResponse(
        val id: Int,
        val title: String,
        val time: String,
        val frequency: String,
        val day_of_week: Int?,
        val day_of_month: Int?,
        val end_date: String?,
        val job_id: String?,
        val created_at: String?
    )
    
    data class GetNotificationsResponse(
        val notifications: List<NotificationResponse>,
        val count: Int
    )
      data class ScheduleResponse(
        val message: String,
        val job_id: String,
        val frequency: String,
        val time: String,
        val day_of_week: Int?,
        val day_of_month: Int?,
        val end_date: String?
    )
    
    suspend fun scheduleNotification(request: ScheduleRequest): Result<ScheduleResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("token", request.token)
                    put("title", request.title)
                    put("time", request.time)
                    put("frequency", request.frequency)
                    request.weekday?.let { put("day_of_week", it) }
                    request.monthday?.let { put("day_of_month", it) }
                    request.enddate?.let { put("end_date", it) }
                }
                
                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaType())
                
                val httpRequest = Request.Builder()
                    .url("$baseUrl/schedule-notification")
                    .post(requestBody)
                    .build()
                
                Log.d("ApiService", "Sending request: $json")
                
                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: ""
                
                Log.d("ApiService", "Response code: ${response.code}")
                Log.d("ApiService", "Response body: $responseBody")
                
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseBody)
                    val scheduleResponse = ScheduleResponse(
                        message = jsonResponse.getString("message"),
                        job_id = jsonResponse.getString("job_id"),
                        frequency = jsonResponse.getString("frequency"),
                        time = jsonResponse.getString("time"),
                        day_of_week = if (jsonResponse.isNull("day_of_week")) null else jsonResponse.getInt("day_of_week"),
                        day_of_month = if (jsonResponse.isNull("day_of_month")) null else jsonResponse.getInt("day_of_month"),
                        end_date = if (jsonResponse.isNull("end_date")) null else jsonResponse.getString("end_date")
                    )
                    Result.success(scheduleResponse)
                } else {
                    Result.failure(Exception("Failed to schedule notification: ${response.code} - $responseBody"))
                }
            } catch (e: IOException) {
                Log.e("ApiService", "Network error", e)
                Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e("ApiService", "Unexpected error", e)
                Result.failure(Exception("Unexpected error: ${e.message}"))
            }
        }
    }
    
    suspend fun getNotifications(token: String): Result<GetNotificationsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val httpRequest = Request.Builder()
                    .url("$baseUrl/get-notifications/$token")
                    .get()
                    .build()
                
                Log.d("ApiService", "Fetching notifications for token: ${token.take(8)}...")
                
                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: ""
                
                Log.d("ApiService", "Get notifications response code: ${response.code}")
                Log.d("ApiService", "Get notifications response body: $responseBody")
                
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseBody)
                    val notificationsArray = jsonResponse.getJSONArray("notifications")
                    val count = jsonResponse.getInt("count")
                    
                    val notifications = mutableListOf<NotificationResponse>()
                    for (i in 0 until notificationsArray.length()) {
                        val notifJson = notificationsArray.getJSONObject(i)
                        notifications.add(
                            NotificationResponse(
                                id = notifJson.getInt("id"),
                                title = notifJson.getString("title"),
                                time = notifJson.getString("time"),
                                frequency = notifJson.getString("frequency"),
                                day_of_week = if (notifJson.isNull("day_of_week")) null else notifJson.getInt("day_of_week"),
                                day_of_month = if (notifJson.isNull("day_of_month")) null else notifJson.getInt("day_of_month"),
                                end_date = if (notifJson.isNull("end_date")) null else notifJson.getString("end_date"),
                                job_id = if (notifJson.isNull("job_id")) null else notifJson.getString("job_id"),
                                created_at = if (notifJson.isNull("created_at")) null else notifJson.getString("created_at")
                            )
                        )
                    }
                    
                    Result.success(GetNotificationsResponse(notifications, count))
                } else {
                    Result.failure(Exception("Failed to get notifications: ${response.code} - $responseBody"))
                }
            } catch (e: IOException) {
                Log.e("ApiService", "Network error getting notifications", e)
                Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e("ApiService", "Unexpected error getting notifications", e)
                Result.failure(Exception("Unexpected error: ${e.message}"))
            }
        }
    }
      suspend fun registerToken(token: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("token", token)
                }
                
                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaType())
                
                val httpRequest = Request.Builder()
                    .url("$baseUrl/register-token")
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: ""
                
                if (response.isSuccessful) {
                    Result.success("Token registered successfully!")
                } else {
                    Result.failure(Exception("Failed to register token: ${response.code} - $responseBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Error registering token: ${e.message}"))
            }
        }
    }
    
    suspend fun deleteNotification(jobId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val httpRequest = Request.Builder()
                    .url("$baseUrl/scheduled-jobs/$jobId")
                    .delete()
                    .build()
                
                Log.d("ApiService", "Deleting notification with job_id: $jobId")
                
                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: ""
                
                Log.d("ApiService", "Delete response code: ${response.code}")
                Log.d("ApiService", "Delete response body: $responseBody")
                
                if (response.isSuccessful) {
                    Result.success("Notification deleted successfully!")
                } else {
                    Result.failure(Exception("Failed to delete notification: ${response.code} - $responseBody"))
                }
            } catch (e: IOException) {
                Log.e("ApiService", "Network error deleting notification", e)
                Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e("ApiService", "Unexpected error deleting notification", e)
                Result.failure(Exception("Unexpected error: ${e.message}"))
            }
        }
    }

    // Tone-related API methods
    suspend fun getTones(): Result<List<com.example.nudger.models.ToneOption>> {
        return withContext(Dispatchers.IO) {
            try {
                val httpRequest = Request.Builder()
                    .url("$baseUrl/tones")
                    .get()
                    .build()
                
                Log.d("ApiService", "Fetching available tones...")
                
                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: ""
                
                Log.d("ApiService", "Get tones response code: ${response.code}")
                Log.d("ApiService", "Get tones response body: $responseBody")
                
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseBody)
                    val tonesArray = jsonResponse.getJSONArray("tones")
                      val tones = mutableListOf<com.example.nudger.models.ToneOption>()
                    for (i in 0 until tonesArray.length()) {
                        val toneJson = tonesArray.getJSONObject(i)
                        tones.add(
                            com.example.nudger.models.ToneOption(
                                toneId = toneJson.getInt("tone_id"),
                                toneName = toneJson.getString("tone_name"),
                                displayName = toneJson.getString("display_name"),
                                imageUrl = toneJson.optString("image_url", null.toString())
                            )
                        )
                    }
                    
                    Result.success(tones)
                } else {
                    Result.failure(Exception("Failed to get tones: ${response.code} - $responseBody"))
                }
            } catch (e: IOException) {
                Log.e("ApiService", "Network error getting tones", e)
                Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e("ApiService", "Unexpected error getting tones", e)
                Result.failure(Exception("Unexpected error: ${e.message}"))
            }
        }
    }

    suspend fun getUserTone(token: String): Result<com.example.nudger.models.TonePreference> {
        return withContext(Dispatchers.IO) {
            try {
                val httpRequest = Request.Builder()
                    .url("$baseUrl/user-tone/$token")
                    .get()
                    .build()
                
                Log.d("ApiService", "Fetching user tone for token: ${token.take(8)}...")
                
                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: ""
                
                Log.d("ApiService", "Get user tone response code: ${response.code}")
                Log.d("ApiService", "Get user tone response body: $responseBody")
                
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseBody)
                    val tonePreference = com.example.nudger.models.TonePreference(
                        token = jsonResponse.getString("token"),
                        tone = jsonResponse.getString("tone"),
                        toneId = jsonResponse.getInt("tone_id"),
                        isDefault = jsonResponse.getBoolean("is_default")
                    )
                    
                    Result.success(tonePreference)
                } else {
                    Result.failure(Exception("Failed to get user tone: ${response.code} - $responseBody"))
                }
            } catch (e: IOException) {
                Log.e("ApiService", "Network error getting user tone", e)
                Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e("ApiService", "Unexpected error getting user tone", e)
                Result.failure(Exception("Unexpected error: ${e.message}"))
            }
        }
    }

    suspend fun setUserTone(token: String, toneId: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("token", token)
                    put("tone_id", toneId)
                }
                
                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaType())
                
                val httpRequest = Request.Builder()
                    .url("$baseUrl/user-tone")
                    .post(requestBody)
                    .build()
                
                Log.d("ApiService", "Setting user tone: $json")
                
                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: ""
                
                Log.d("ApiService", "Set user tone response code: ${response.code}")
                Log.d("ApiService", "Set user tone response body: $responseBody")
                
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseBody)
                    Result.success(jsonResponse.getString("message"))
                } else {
                    Result.failure(Exception("Failed to set user tone: ${response.code} - $responseBody"))
                }
            } catch (e: IOException) {
                Log.e("ApiService", "Network error setting user tone", e)
                Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e("ApiService", "Unexpected error setting user tone", e)
                Result.failure(Exception("Unexpected error: ${e.message}"))
            }
        }
    }
}
