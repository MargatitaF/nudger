package com.example.nudger.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nudger.models.FrequencyType
import com.example.nudger.models.NotificationPreference
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationConfigDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSave: (NotificationPreference) -> Unit,
    onDelete: ((NotificationPreference) -> Unit)? = null,
    existingPreference: NotificationPreference? = null
) {
    if (!isVisible) return

    var title by remember { mutableStateOf(existingPreference?.title ?: "") }
    var time by remember { mutableStateOf(existingPreference?.time ?: "09:00") }
    var selectedFrequency by remember { mutableStateOf(existingPreference?.frequency ?: FrequencyType.DAILY) }
    var selectedDaysOfWeek by remember { mutableStateOf(existingPreference?.daysOfWeek ?: emptySet()) }
    var dayOfMonth by remember { mutableStateOf(existingPreference?.dayOfMonth?.toString() ?: "") }
    var endDate by remember { mutableStateOf(existingPreference?.endDate ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var canShowEndDatePicker by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hour: Int, minute: Int ->
            time = String.format("%02d:%02d", hour, minute)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
    )

    val endDatePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year: Int, month: Int, dayOfMonth: Int ->
            endDate = String.format("%02d-%02d-%d", dayOfMonth, month + 1, year)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val dayOfMonthPickerDialog = android.app.DatePickerDialog(
        context,
        { _, _, _, day: Int ->
            dayOfMonth = day.toString()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with title and delete button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {                    Text(
                        text = if (existingPreference != null) "Edit Notification" else "New Nudge",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    if (existingPreference != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(existingPreference)
                                onDismiss()
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete notification",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Time field
                Box(modifier = Modifier.clickable { timePickerDialog.show() }) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { },
                        label = { Text("Time") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                        enabled = false,
                        trailingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = "Select time")
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Frequency dropdown                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedFrequency.value.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },                        
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        FrequencyType.entries.forEach { frequency ->
                            DropdownMenuItem(
                                text = { Text(frequency.value.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedFrequency = frequency
                                    expanded = false
                                    // Clear day selections when frequency changes
                                    if (frequency != FrequencyType.WEEKLY) {
                                        selectedDaysOfWeek = emptySet()
                                    }
                                    if (frequency != FrequencyType.MONTHLY) {
                                        dayOfMonth = ""
                                    }
                                }
                            )
                        }
                    }
                }

                // Days of week selection (for weekly frequency)
                if (selectedFrequency == FrequencyType.WEEKLY) {
                    Text("Select days of the week:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                        dayLabels.forEachIndexed { index, dayLabel ->
                            val dayNumber = index + 1
                            val isSelected = selectedDaysOfWeek.contains(dayNumber)

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable {
                                        selectedDaysOfWeek = if (isSelected) {
                                            selectedDaysOfWeek - dayNumber
                                        } else {
                                            selectedDaysOfWeek + dayNumber
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                ),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {                                    Text(
                                        text = dayLabel,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                // Day of month field (for monthly frequency)
                if (selectedFrequency == FrequencyType.MONTHLY) {
                    Box(modifier = Modifier.clickable { dayOfMonthPickerDialog.show() }) {
                        OutlinedTextField(
                            value = dayOfMonth,
                            onValueChange = { },
                            label = { Text("Day of Month") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            singleLine = true,
                            enabled = false,
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Select day of month")
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // End date field (optional)
                if (selectedFrequency != FrequencyType.ONCE) {
                    Box(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (canShowEndDatePicker) {
                            endDatePickerDialog.show()
                        }
                        canShowEndDatePicker = true
                    }) {
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { },
                            label = { Text("End Date (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            singleLine = true,
                            enabled = false,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (endDate.isNotEmpty()) {
                                        IconButton(onClick = {
                                            endDate = ""
                                            canShowEndDatePicker = false
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear end date")
                                        }
                                    }
                                    Icon(Icons.Default.DateRange, contentDescription = "Select end date")
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {                    
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = {
                            val preference = NotificationPreference(
                                title = title,
                                time = time,
                                frequency = selectedFrequency,
                                daysOfWeek = selectedDaysOfWeek,
                                dayOfMonth = dayOfMonth.toIntOrNull(),
                                endDate = endDate.ifEmpty { null }
                            )
                            onSave(preference)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && time.isNotBlank()
                    ) {
                        Text(
                            text = "Save",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
