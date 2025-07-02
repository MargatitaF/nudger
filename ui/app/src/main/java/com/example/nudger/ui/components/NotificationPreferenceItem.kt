package com.example.nudger.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nudger.models.FrequencyType
import com.example.nudger.models.NotificationPreference

@Composable
fun NotificationPreferenceItem(
    preference: NotificationPreference,
    onEdit: (NotificationPreference) -> Unit = {},
    onDelete: (NotificationPreference) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit(preference) },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title and Time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {                Text(
                    text = preference.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = preference.time,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
              // Frequency description
            Text(
                text = buildFrequencyDescription(preference),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Days of week (for weekly frequency)
            if (preference.frequency == FrequencyType.WEEKLY && preference.daysOfWeek.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDaysOfWeek(preference.daysOfWeek),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // End date (if specified)
            preference.endDate?.let { endDate ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Until: $endDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action buttons (for future implementation)
            /*
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onEdit(preference) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { onDelete(preference) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            */
        }
    }
}

private fun buildFrequencyDescription(preference: NotificationPreference): String {
    return when (preference.frequency) {
        FrequencyType.ONCE -> "Occurs once"
        FrequencyType.DAILY -> "Occurs daily"
        FrequencyType.WEEKLY -> {
            if (preference.daysOfWeek.isEmpty()) {
                "Occurs weekly"
            } else {
                "Occurs weekly on selected days"
            }
        }
        FrequencyType.MONTHLY -> {
            preference.dayOfMonth?.let { day ->
                "Occurs monthly on day $day"
            } ?: "Occurs monthly"
        }
        FrequencyType.WEEKDAYS -> "Occurs on weekdays (Mon-Fri)"
        FrequencyType.WEEKENDS -> "Occurs on weekends (Sat-Sun)"
    }
}

private fun formatDaysOfWeek(daysOfWeek: Set<Int>): String {
    val dayLabels = mapOf(
        1 to "Mo", 2 to "Tu", 3 to "We", 4 to "Th", 
        5 to "Fr", 6 to "Sa", 7 to "Su"
    )
    return daysOfWeek.sorted().mapNotNull { dayLabels[it] }.joinToString(" ")
}
