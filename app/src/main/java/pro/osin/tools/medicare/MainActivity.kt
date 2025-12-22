package pro.osin.tools.medicare

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pro.osin.tools.medicare.R
import pro.osin.tools.medicare.data.database.MedicineDatabase
import pro.osin.tools.medicare.data.repository.MedicineRepository
import pro.osin.tools.medicare.data.repository.ReminderRepository
import pro.osin.tools.medicare.ui.navigation.NavGraph
import pro.osin.tools.medicare.ui.theme.MedicareappTheme
import pro.osin.tools.medicare.util.LocaleHelper
import pro.osin.tools.medicare.util.NotificationHelper
import pro.osin.tools.medicare.util.PreferencesManager
import pro.osin.tools.medicare.util.ReminderScheduler
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        val preferencesManager = PreferencesManager(newBase!!)
        // Use runBlocking to get value from Flow synchronously
        val language = try {
            runBlocking {
                preferencesManager.language.first()
            }
        } catch (e: Exception) {
            PreferencesManager.LANGUAGE_RU
        }
        val context = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = MedicineDatabase.getDatabase(applicationContext)
        val medicineRepository = MedicineRepository(database.medicineDao())
        val reminderRepository = ReminderRepository(database.reminderDao(), applicationContext)
        val preferencesManager = PreferencesManager(applicationContext)

        // Create notification channel
        NotificationHelper.createNotificationChannel(applicationContext)
        
        // Schedule periodic reminder check
        ReminderScheduler.schedulePeriodicCheck(applicationContext)
        
        // Restore all active reminders on startup
        // Use lifecycleScope or application scope to avoid memory leaks
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val reminders = reminderRepository.getAllActiveReminders().first()
                reminders.forEach { reminder ->
                    ReminderScheduler.scheduleReminder(applicationContext, reminder)
                }
            } catch (e: Exception) {
                // Ignore errors during restoration
            }
        }
        // Note: This scope is intentionally not cancelled as it's a one-time operation
        // and should complete even if activity is destroyed

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            
            // Get theme and language settings
            val theme by preferencesManager.theme.collectAsState(initial = PreferencesManager.THEME_SYSTEM)
            val language by preferencesManager.language.collectAsState(initial = PreferencesManager.LANGUAGE_RU)
            val batteryOptimizationDialogShown by preferencesManager.batteryOptimizationDialogShown.collectAsState(initial = false)
            val disclaimerAccepted by preferencesManager.disclaimerAccepted.collectAsState(initial = false)
            
            val scope = rememberCoroutineScope()
            
            // State for showing disclaimer dialog
            var showDisclaimerDialog by remember { mutableStateOf(false) }
            
            // Check disclaimer acceptance on startup - show dialog first if not accepted
            LaunchedEffect(disclaimerAccepted) {
                if (!disclaimerAccepted) {
                    showDisclaimerDialog = true
                }
            }
            
            // Check and request notification permission
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Permission granted or denied - continue with app initialization
            }
            
            // State for showing DND permission dialog
            var showDndPermissionDialog by remember { mutableStateOf(false) }
            // State to track if DND check is completed
            var dndCheckCompleted by remember { mutableStateOf(false) }
            // State for showing battery optimization dialog
            var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
            
            // Function to check battery optimization and show dialog only on first launch
            // Always show dialog on first launch regardless of system battery optimization status,
            // as device manufacturers (Samsung, Huawei, Honor, Xiaomi, etc.) have their own battery managers
            val checkBatteryOptimization = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !batteryOptimizationDialogShown) {
                    // Show dialog only if it hasn't been shown before
                    showBatteryOptimizationDialog = true
                    // Mark dialog as shown
                    scope.launch {
                        preferencesManager.setBatteryOptimizationDialogShown(true)
                    }
                }
            }
            
            // Check and request notification policy access (Do Not Disturb)
            val notificationPolicyLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                // User returned from settings - mark DND check as completed
                dndCheckCompleted = true
                // Check battery optimization after returning from DND settings
                checkBatteryOptimization()
            }
            
            // Launcher for battery optimization settings
            val batteryOptimizationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                // User returned from battery optimization settings
                // Dialog was already shown on first launch, no need to show again
            }
            
            // Function to open DND settings
            val openDndSettings = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                notificationPolicyLauncher.launch(intent)
            }
            
            // Function to open battery optimization settings
            val openBatteryOptimizationSettings = {
                val packageName = context.packageName
                // Try to request permission directly (may not work on all devices)
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:$packageName".toUri()
                    }
                    batteryOptimizationLauncher.launch(intent)
                } catch (e: Exception) {
                    // If direct request fails, open battery optimization settings page
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        batteryOptimizationLauncher.launch(intent)
                    } catch (e2: Exception) {
                        // Fallback: open app info page where user can manage battery optimization
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:$packageName".toUri()
                        }
                        batteryOptimizationLauncher.launch(intent)
                    }
                }
            }
            
            // Check notification permission on startup (only if disclaimer is accepted)
            LaunchedEffect(disclaimerAccepted) {
                if (!disclaimerAccepted) return@LaunchedEffect
                
                // Check POST_NOTIFICATIONS permission (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                
                // Check notification policy access (Do Not Disturb) - Android 5.0+
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val hasPolicyAccess = notificationManager.isNotificationPolicyAccessGranted
                    if (!hasPolicyAccess) {
                        // Show dialog first, then open settings
                        showDndPermissionDialog = true
                        // Don't check battery optimization yet - wait for DND to complete
                        return@LaunchedEffect
                    }
                }
                // If DND access is already granted, mark as completed
                dndCheckCompleted = true
                
                // Check battery optimization immediately if DND is already set up
                checkBatteryOptimization()
                
                // Navigate to settings screen after restart
                val navigateTo = intent.getStringExtra("navigate_to")
                if (navigateTo != null) {
                    navController.navigate(navigateTo) {
                        // Pop to start destination but keep it in the stack
                        // This allows back button to work properly
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = false
                        }
                        // Avoid multiple copies of the same screen
                        launchSingleTop = true
                    }
                }
            }
            
            
            val isDarkTheme = when (theme) {
                PreferencesManager.THEME_DARK -> true
                PreferencesManager.THEME_LIGHT -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MedicareappTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Only show app content if disclaimer is accepted
                    if (disclaimerAccepted) {
                        NavGraph(navController = navController)
                    }
                }
                
                // Disclaimer Dialog - must be shown first and cannot be dismissed
                if (showDisclaimerDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            // Cannot dismiss - user must accept or decline
                        },
                        title = { Text(stringResource(R.string.disclaimer_title)) },
                        text = { Text(stringResource(R.string.disclaimer_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDisclaimerDialog = false
                                    // Save acceptance
                                    scope.launch {
                                        preferencesManager.setDisclaimerAccepted(true)
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.disclaimer_accept))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    // Close the app if user declines
                                    finish()
                                }
                            ) {
                                Text(stringResource(R.string.disclaimer_decline))
                            }
                        }
                    )
                }
                
                // DND Permission Dialog
                if (showDndPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { 
                            showDndPermissionDialog = false
                            // If user dismisses dialog, mark DND check as completed to proceed with battery check
                            dndCheckCompleted = true
                        },
                        title = { Text(stringResource(R.string.dnd_permission_title)) },
                        text = { Text(stringResource(R.string.dnd_permission_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDndPermissionDialog = false
                                    openDndSettings()
                                }
                            ) {
                                Text(stringResource(R.string.dnd_permission_continue))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { 
                                showDndPermissionDialog = false
                                // If user dismisses dialog, mark DND check as completed to proceed with battery check
                                dndCheckCompleted = true
                            }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
                
                // Battery Optimization Dialog
                if (showBatteryOptimizationDialog) {
                    AlertDialog(
                        onDismissRequest = { showBatteryOptimizationDialog = false },
                        title = { Text(stringResource(R.string.battery_optimization_title)) },
                        text = { Text(stringResource(R.string.battery_optimization_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showBatteryOptimizationDialog = false
                                    openBatteryOptimizationSettings()
                                }
                            ) {
                                Text(stringResource(R.string.battery_optimization_continue))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBatteryOptimizationDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}
