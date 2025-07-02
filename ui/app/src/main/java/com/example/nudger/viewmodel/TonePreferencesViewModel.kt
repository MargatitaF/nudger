package com.example.nudger.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.nudger.ApiService
import com.example.nudger.models.ToneOption
import com.example.nudger.models.TonePreference
import android.util.Log

class TonePreferencesViewModel(
    private val apiService: ApiService,
    private val fcmToken: String
) : ViewModel() {
    
    private val _availableTones = mutableStateListOf<ToneOption>()
    val availableTones: List<ToneOption> = _availableTones
    
    private val _currentTonePreference = mutableStateOf<TonePreference?>(null)
    private val _isLoading = mutableStateOf(false)
    val isLoading: Boolean by _isLoading
    
    private val _statusMessage = mutableStateOf("")
    val statusMessage: String by _statusMessage
    
    private val _selectedToneId = mutableStateOf<Int?>(null)
    val selectedToneId: Int? by _selectedToneId
    
    init {
        // Load available tones and current user preference when ViewModel is created
        loadAvailableTones()
        loadCurrentTonePreference()
    }
    
    private fun loadAvailableTones() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Loading tone options..."
            
            try {
                val result = apiService.getTones()
                result.onSuccess { tones ->
                    _availableTones.clear()
                    _availableTones.addAll(tones)
                    _statusMessage.value = ""
                    Log.d("ToneViewModel", "Loaded ${tones.size} tone options")
                }.onFailure { error ->
                    _statusMessage.value = "Error loading tones: ${error.message}"
                    Log.e("ToneViewModel", "Failed to load tones", error)
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading tones: ${e.message}"
                Log.e("ToneViewModel", "Exception loading tones", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadCurrentTonePreference() {
        if (fcmToken.isEmpty()) {
            _statusMessage.value = "Waiting for FCM token..."
            return
        }
        
        viewModelScope.launch {
            try {
                val result = apiService.getUserTone(fcmToken)
                result.onSuccess { preference ->
                    _currentTonePreference.value = preference
                    _selectedToneId.value = preference.toneId
                    Log.d("ToneViewModel", "Current tone: ${preference.tone} (ID: ${preference.toneId})")
                }.onFailure { error ->
                    _statusMessage.value = "Error loading current tone: ${error.message}"
                    Log.e("ToneViewModel", "Failed to load current tone", error)
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading current tone: ${e.message}"
                Log.e("ToneViewModel", "Exception loading current tone", e)
            }
        }
    }
    
    fun selectTone(toneId: Int) {
        if (fcmToken.isEmpty()) {
            _statusMessage.value = "FCM token not available"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = apiService.setUserTone(fcmToken, toneId)
                result.onSuccess {
                    _selectedToneId.value = toneId
                    val selectedTone = _availableTones.find { it.toneId == toneId }
                    _currentTonePreference.value = TonePreference(
                        token = fcmToken,
                        tone = selectedTone?.toneName ?: "unknown",
                        toneId = toneId,
                        isDefault = false
                    )
                    Log.d("ToneViewModel", "Tone updated to ID: $toneId")
                }.onFailure { error ->
                    _statusMessage.value = "Error updating tone: ${error.message}"
                    Log.e("ToneViewModel", "Failed to update tone", error)
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error updating tone: ${e.message}"
                Log.e("ToneViewModel", "Exception updating tone", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getToneColor(toneName: String): androidx.compose.ui.graphics.Color {
        return when (toneName.lowercase()) {
            "caring" -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
            "neutral" -> androidx.compose.ui.graphics.Color(0xFF9E9E9E) // Gray
            "assertive" -> androidx.compose.ui.graphics.Color(0xFFFF5722) // Red-Orange
            "encouraging" -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
            else -> androidx.compose.ui.graphics.Color(0xFF9E9E9E) // Default gray
        }
    }
}
