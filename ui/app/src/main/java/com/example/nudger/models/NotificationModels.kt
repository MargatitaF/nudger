package com.example.nudger.models

enum class FrequencyType(val value: String) {
    ONCE("once"),
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    WEEKDAYS("weekdays"),
    WEEKENDS("weekends");

    override fun toString(): String = value

    companion object {
        fun fromString(value: String): FrequencyType? {
            return values().find { it.value == value }
        }
    }
}

// Tone-related data classes
data class ToneOption(
    val toneId: Int,
    val toneName: String,
    val displayName: String,
    val imageUrl: String? = null
)

data class TonePreference(
    val token: String,
    val tone: String,
    val toneId: Int,
    val isDefault: Boolean
)

data class NotificationPreference(
    val id: String = "",
    val title: String = "",
    val time: String = "", // HH:MM format
    val frequency: FrequencyType = FrequencyType.DAILY,
    val daysOfWeek: Set<Int> = emptySet(), // 1-7 for Mon-Sun
    val dayOfMonth: Int? = null, // 1-31 for monthly
    val endDate: String? = null, // DD-MM-YYYY format
    val jobIds: List<String> = emptyList() // APScheduler job IDs for deletion (can be multiple for weekly)
)

data class NotificationPreferenceRequest(
    val token: String,
    val title: String,
    val time: String,
    val frequency: String,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val endDate: String? = null
)
