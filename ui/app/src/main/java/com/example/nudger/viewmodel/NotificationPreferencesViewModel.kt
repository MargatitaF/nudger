package com.example.nudger.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nudger.ApiService
import com.example.nudger.models.FrequencyType
import com.example.nudger.models.NotificationPreference
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class NotificationPreferencesViewModel(
    private val apiService: ApiService,
    private val fcmToken: String
) : ViewModel() {
    
    private val _preferences = mutableStateListOf<NotificationPreference>()
    val preferences: List<NotificationPreference> = _preferences
    
    private val _isLoading = mutableStateOf(false)
    val isLoading get() = _isLoading.value
    
    private val _statusMessage = mutableStateOf("")
    val statusMessage get() = _statusMessage.value
    private val _showDialog = mutableStateOf(false)
    val showDialog get() = _showDialog.value
    
    private val _editingPreference = mutableStateOf<NotificationPreference?>(null)
    val editingPreference get() = _editingPreference.value
    
    init {
        // Load existing preferences when ViewModel is created
        loadExistingPreferences()
    }
    fun showConfigDialog() {
        _showDialog.value = true
    }
    
    fun hideConfigDialog() {
        _showDialog.value = false
        _editingPreference.value = null
    }
    
    fun startEditingPreference(preference: NotificationPreference) {
        _editingPreference.value = preference
    }    fun addPreference(preference: NotificationPreference) {
        viewModelScope.launch {
            _isLoading.value = true
            val isEditing = _editingPreference.value != null
            
            if (isEditing) {
                _statusMessage.value = "Updating notification..."
                // Delete the old notification jobs from backend first
                val oldPreference = _editingPreference.value!!
                if (oldPreference.jobIds.isNotEmpty()) {
                    var allDeleted = true
                    for (jobId in oldPreference.jobIds) {
                        val deleteResult = apiService.deleteNotification(jobId)
                        deleteResult.onFailure { 
                            allDeleted = false
                        }
                    }
                    if (!allDeleted) {
                        _statusMessage.value = "Warning: Some old jobs couldn't be deleted. Continuing with update..."
                    }
                }
                // Remove old preference from local list
                _preferences.remove(oldPreference)
            } else {
                _statusMessage.value = "Scheduling notification..."
            }
              try {
                val requests = convertToApiRequests(preference)
                var allSuccessful = true
                val errorMessages = mutableListOf<String>()
                val collectedJobIds = mutableListOf<String>()
                
                // Send all requests for this preference
                for (request in requests) {
                    val result = apiService.scheduleNotification(request)
                    result.onSuccess { response ->
                        collectedJobIds.add(response.job_id)
                    }.onFailure { error ->
                        allSuccessful = false
                        errorMessages.add(error.message ?: "Unknown error")
                    }
                }
                
                if (allSuccessful) {
                    // Add the new preference to local list with collected job_ids
                    _preferences.add(preference.copy(id = generateId(), jobIds = collectedJobIds))
                    _statusMessage.value = if (isEditing) {
                        "✓ '${preference.title}' updated successfully"
                    } else {
                        "✓ '${preference.title}' scheduled ${getFrequencyText(preference)} at ${preference.time}"
                    }
                    _showDialog.value = false
                    _editingPreference.value = null
                    autoClearSuccessMessage()
                } else {
                    _statusMessage.value = "Some notifications failed: ${errorMessages.joinToString(", ")}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }fun removePreference(preference: NotificationPreference) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Removing notification..."
            
            try {
                if (preference.jobIds.isNotEmpty()) {
                    var allSuccessful = true
                    val errorMessages = mutableListOf<String>()
                    
                    // Delete all jobs associated with this preference
                    for (jobId in preference.jobIds) {
                        val result = apiService.deleteNotification(jobId)
                        result.onFailure { error ->
                            allSuccessful = false
                            errorMessages.add("Job $jobId: ${error.message}")
                        }
                    }
                    
                    if (allSuccessful) {
                        // Remove from local list only if all API calls succeeded
                        _preferences.remove(preference)
                        _statusMessage.value = "✓ '${preference.title}' removed successfully"
                        autoClearSuccessMessage()
                    } else {
                        _statusMessage.value = "Some deletions failed: ${errorMessages.joinToString(", ")}"
                    }
                } else {
                    // If no job_ids, just remove from local list (fallback)
                    _preferences.remove(preference)
                    _statusMessage.value = "✓ '${preference.title}' removed successfully (no job_ids found)"
                    autoClearSuccessMessage()
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error removing notification: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
      private fun loadExistingPreferences() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Loading existing notifications..."
            
            try {
                val result = apiService.getNotifications(fcmToken)
                result.onSuccess { response ->
                    _preferences.clear()
                    
                    // Group notifications by title, time, frequency, and end_date to handle weekly multi-day notifications
                    val groupedNotifications = response.notifications.groupBy { notification ->
                        "${notification.title}_${notification.time}_${notification.frequency}_${notification.end_date}"
                    }
                      groupedNotifications.forEach { (_, notifications) ->
                        val firstNotif = notifications.first()
                        
                        // For weekly notifications, collect all days of week
                        val daysOfWeek = if (firstNotif.frequency.lowercase() == "weekly") {
                            notifications.mapNotNull { it.day_of_week }.toSet()
                        } else {
                            firstNotif.day_of_week?.let { setOf(it) } ?: emptySet()
                        }
                        
                        // Collect all job_ids for this preference
                        val jobIds = notifications.mapNotNull { it.job_id }
                        
                        val preference = NotificationPreference(
                            id = firstNotif.id.toString(),
                            title = firstNotif.title,
                            time = firstNotif.time,
                            frequency = FrequencyType.valueOf(firstNotif.frequency.uppercase()),
                            daysOfWeek = daysOfWeek,
                            dayOfMonth = firstNotif.day_of_month,
                            endDate = firstNotif.end_date,
                            jobIds = jobIds
                        )
                        _preferences.add(preference)
                    }
                    
                    _statusMessage.value = if (_preferences.isNotEmpty()) {
                        "✓ Loaded ${_preferences.size} existing notification(s)"
                    } else {
                        ""
                    }
                    autoClearSuccessMessage()                }.onFailure { error ->
                    // Check if it's a 404 error (no notifications found) - this is normal
                    if (error.message?.contains("404") == true) {
                        _statusMessage.value = ""
                        _preferences.clear()
                    } else {
                        _statusMessage.value = "Error loading notifications: ${error.message}"
                    }
                }            } catch (e: Exception) {
                // Check if it's a 404 error (no notifications found) - this is normal
                if (e.message?.contains("404") == true) {
                    _statusMessage.value = ""
                    _preferences.clear()
                } else {
                    _statusMessage.value = "Error loading notifications: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
      private fun convertToApiRequests(preference: NotificationPreference): List<ApiService.ScheduleRequest> {
        return when (preference.frequency) {
            FrequencyType.WEEKLY -> {
                if (preference.daysOfWeek.isNotEmpty()) {
                    // Create separate requests for each selected day
                    preference.daysOfWeek.map { dayOfWeek ->
                        ApiService.ScheduleRequest(
                            token = fcmToken,
                            title = preference.title,
                            time = preference.time,
                            frequency = preference.frequency.value,
                            weekday = dayOfWeek,
                            monthday = null,
                            enddate = preference.endDate
                        )
                    }
                } else {
                    // No specific days selected, create a single weekly request
                    listOf(
                        ApiService.ScheduleRequest(
                            token = fcmToken,
                            title = preference.title,
                            time = preference.time,
                            frequency = preference.frequency.value,
                            weekday = null,
                            monthday = null,
                            enddate = preference.endDate
                        )
                    )
                }
            }
            else -> {
                // For all other frequencies, create a single request
                listOf(
                    ApiService.ScheduleRequest(
                        token = fcmToken,
                        title = preference.title,
                        time = preference.time,
                        frequency = preference.frequency.value,
                        weekday = null,
                        monthday = preference.dayOfMonth,
                        enddate = preference.endDate
                    )
                )
            }
        }
    }
    
    private fun generateId(): String {
        return System.currentTimeMillis().toString()
    }
      private fun clearStatusMessage() {
        _statusMessage.value = ""
    }
    
    // Add method to refresh status when dialog opens
    fun onDialogOpened() {
        clearStatusMessage()
    }

    // Method to get detailed success message
    private fun getFrequencyText(preference: NotificationPreference): String {
        return when (preference.frequency) {
            FrequencyType.ONCE -> "once"
            FrequencyType.DAILY -> "daily"
            FrequencyType.WEEKLY -> if (preference.daysOfWeek.isNotEmpty()) {
                "weekly on ${preference.daysOfWeek.size} selected days"
            } else "weekly"
            FrequencyType.MONTHLY -> "monthly"
            FrequencyType.WEEKDAYS -> "on weekdays"
            FrequencyType.WEEKENDS -> "on weekends"
        }
    }
    
    // Auto-clear success messages after delay
    private fun autoClearSuccessMessage() {
        if (_statusMessage.value.startsWith("✓")) {
            viewModelScope.launch {
                delay(3000) // Clear after 3 seconds
                if (_statusMessage.value.startsWith("✓")) {
                    _statusMessage.value = ""
                }
            }
        }
    }
}
