package com.example.nudger

import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material.icons.filled.Add
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.nudger.ui.theme.NudgerTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nudger.viewmodel.NotificationPreferencesViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

@Composable
fun Logo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // You can add an actual logo image here in the future
        // For now, we'll use styled text        
        Text(
            text = "Nudger",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun HomeIcon(
    modifier: Modifier = Modifier, 
    onClick: () -> Unit,
    isVisible: Boolean = true
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = if (isVisible) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isVisible) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (isVisible) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Home",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ToneIcon(
    modifier: Modifier = Modifier, 
    onClick: () -> Unit,
    isVisible: Boolean = true
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = if (isVisible) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isVisible) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (isVisible) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Select Tone",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}


@Composable
fun PageHeader(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(top = 35.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Right side - Home button when not on main page, Settings button when on main page
            if (currentRoute == "notification_preferences") {
                ToneIcon(
                    modifier = Modifier.padding(end = 4.dp),
                    onClick = { navController.navigate("tone_preferences") },
                    isVisible = true
                )
            } else {
                HomeIcon(
                    modifier = Modifier.padding(end = 4.dp),
                    onClick = { navController.navigate("notification_preferences") },
                    isVisible = true
                )
            }
        }
        
        Logo(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun Main(navController: NavHostController, mainActivity: MainActivity) {
    NavHost(navController = navController, startDestination = "notification_preferences") {
        composable("home") { HomeScreen(navController = navController) }
        composable("notification_preferences") { NotificationPreferencesScreen(navController = navController, mainActivity = mainActivity) }
        composable("tone_preferences") { TonePreferencesScreen(navController = navController, mainActivity = mainActivity) }
    }
}

class MainActivity : ComponentActivity() {
    private var fcmToken: String = ""
    private val apiService = ApiService(this)
    
    private fun getAndSendFCMToken() {
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get token and store it
            fcmToken = task.result
            Log.d("FCM", "FCM Token: $fcmToken")

            // Register token with backend
            lifecycleScope.launch {
                val result = apiService.registerToken(fcmToken)
                result.onSuccess { message ->
                    Log.d("FCM", "Token registered: $message")
                }.onFailure { error ->
                    Log.e("FCM", "Failed to register token: ${error.message}")
                }
            }
        }
    }

    fun getFCMToken(): String = fcmToken
    fun getApiService(): ApiService = apiService

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NudgerTheme {
                val navController = rememberNavController()
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Main(navController = navController, mainActivity = this@MainActivity)
                }
            }
        }
        requestNotificationPermission()
        getAndSendFCMToken()
    }
}


@Composable
fun HomeScreen(navController: NavController) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(navController = navController)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 50.dp, end = 50.dp, bottom = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                //Spacer(modifier = Modifier.weight(1f))
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Btn_NotifPref(onClick = {
                    navController.navigate("notification_preferences")
                })
                Btn_TonePref(onClick = {
                    navController.navigate("tone_preferences")
                })
            }
        }
    }
}

@Composable
fun NotificationPreferencesScreen(navController: NavController, mainActivity: MainActivity) {
    val viewModel: NotificationPreferencesViewModel = viewModel { 
        NotificationPreferencesViewModel(mainActivity.getApiService(), mainActivity.getFCMToken())
    }
      Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageHeader(navController = navController)
                Text("My Nudges",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(top = 20.dp))
                Box(modifier = Modifier.height(20.dp))
                // Status message (always reserve space with fixed height)
                val statusCardHeight = 20.dp
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical =0.dp)
                        .height(statusCardHeight),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.statusMessage.isNotEmpty() && viewModel.statusMessage.contains("Error")) 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        else if (viewModel.statusMessage.isNotEmpty())
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        else
                            Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.statusMessage.isNotEmpty()) {
                            Text(
                                text = viewModel.statusMessage,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                                color = if (viewModel.statusMessage.contains("Error")) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                if (mainActivity.getFCMToken().isEmpty()) {
                    Text(
                        text = "Waiting for FCM token...",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Preferences list
                Column(modifier = Modifier.width(300.dp)) {
                    if (viewModel.preferences.isEmpty()) {
                        Text(
                            text = "You didn't set up any Nudges yet!\nTap the + icon to create one",
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        viewModel.preferences.sortedBy { it.time }.forEach { preference ->
                            com.example.nudger.ui.components.NotificationPreferenceItem(
                                preference = preference,
                                onEdit = { prefToEdit ->
                                    viewModel.startEditingPreference(prefToEdit)
                                    viewModel.showConfigDialog()
                                },
                                onDelete = { prefToDelete ->
                                    viewModel.removePreference(prefToDelete)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
                }
            }
            // Floating Action Button in bottom right corner
            FloatingActionButton(
                onClick = { 
                    viewModel.onDialogOpened()
                    viewModel.showConfigDialog() 
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 40.dp, end = 40.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New Nudge",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
    // Configuration dialog
    com.example.nudger.ui.components.NotificationConfigDialog(
        isVisible = viewModel.showDialog,
        onDismiss = { viewModel.hideConfigDialog() },
        onSave = { preference ->
            viewModel.addPreference(preference)
        },
        onDelete = { preference ->
            viewModel.removePreference(preference)
        },
        existingPreference = viewModel.editingPreference
    )
}




@Composable
fun TonePreferencesScreen(navController: NavController, mainActivity: MainActivity) {
    val viewModel: com.example.nudger.viewmodel.TonePreferencesViewModel = viewModel { 
        com.example.nudger.viewmodel.TonePreferencesViewModel(mainActivity.getApiService(), mainActivity.getFCMToken())
    }
    
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageHeader(navController = navController)
            Text("Moods",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(top = 20.dp))
            
            Text("Choose the default tone for your notifications",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp))            // Status message (minimal gap when empty, expands for message)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .heightIn(min = 5.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.statusMessage.isNotEmpty() && viewModel.statusMessage.contains("Error")) 
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    else if (viewModel.statusMessage.isNotEmpty() && viewModel.statusMessage.startsWith("✓"))
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    else if (viewModel.statusMessage.isNotEmpty())
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else
                        Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.statusMessage.isNotEmpty()) {
                        Text(
                            text = viewModel.statusMessage,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            color = if (viewModel.statusMessage.contains("Error")) 
                                MaterialTheme.colorScheme.error
                            else if (viewModel.statusMessage.startsWith("✓"))
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
              if (mainActivity.getFCMToken().isEmpty()) {
                Text(
                    text = "Waiting for FCM token...",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            ToneTable(viewModel = viewModel)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}


@Composable
fun Btn_NotifPref(onClick: () -> Unit) {    Button(onClick = onClick,  modifier = Modifier.width(300.dp)) {
        Text(
            text = "My Nudges", 
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

@Composable
fun Btn_TonePref(onClick: () -> Unit) {    Button(onClick = onClick,  modifier = Modifier.width(300.dp)) {
        Text(
            text = "Select Tone", 
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

@Composable
fun ToneTable(viewModel: com.example.nudger.viewmodel.TonePreferencesViewModel) {
    if (viewModel.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.padding(20.dp)
        )
    } else {          
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Display tones in a vertical list using the standard layout
            viewModel.availableTones.forEach { tone ->
                ToneOption(
                    tone = tone,
                    isSelected = viewModel.selectedToneId == tone.toneId,
                    onToneSelected = { viewModel.selectTone(tone.toneId) },
                    toneColor = viewModel.getToneColor(tone.toneName)
                )
            }
        }
    }
}

@Composable
fun ToneOption(
    tone: com.example.nudger.models.ToneOption,
    isSelected: Boolean,
    onToneSelected: () -> Unit,
    toneColor: Color
) {    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) toneColor else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isSelected) toneColor.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )              
            .clickable { onToneSelected() }
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Picture on the left
        if (tone.imageUrl != null) {            
            val context = LocalContext.current
            val baseUrl = context.getString(R.string.base_url)
            AsyncImage(
                model = "$baseUrl${tone.imageUrl}",
                contentDescription = tone.displayName,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            // Fallback to colored circle if no image
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = toneColor,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))        // Text on the right (title and description)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {            Text(
                text = tone.displayName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
            )
            
            val personalDescription = when (tone.displayName.lowercase()) {
                "assertive" -> "Direct and confident"
                "caring" -> "Warm and supportive"
                "encouraging" -> "Motivational and uplifting"
                "neutral" -> "Balanced and professional"
                else -> "Perfect for ${tone.displayName.lowercase()} communications"
            }
            
            Text(
                text = personalDescription,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        // Selection indicator on the far right
        androidx.compose.material3.RadioButton(
            selected = isSelected,
            onClick = onToneSelected,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = toneColor,
                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    NudgerTheme {
        PageHeader(navController = rememberNavController())
        HomeScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationPreferencesPreview() {
    NudgerTheme {
        // Create a mock MainActivity for preview
        val mockActivity = MainActivity()
        PageHeader(navController = rememberNavController())
        NotificationPreferencesScreen(navController = rememberNavController(), mainActivity = mockActivity)
        HomeScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun TonePreferencesPreview() {
    NudgerTheme {
        // Create a mock MainActivity for preview
        val mockActivity = MainActivity()
        PageHeader(navController = rememberNavController())
        TonePreferencesScreen(navController = rememberNavController(), mainActivity = mockActivity)
        HomeScreen(navController = rememberNavController())
    }
}