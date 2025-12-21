package pro.osin.tools.medicare

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pro.osin.tools.medicare.data.database.MedicineDatabase
import pro.osin.tools.medicare.data.repository.MedicineRepository
import pro.osin.tools.medicare.data.repository.ReminderRepository
import pro.osin.tools.medicare.ui.navigation.NavGraph
import pro.osin.tools.medicare.ui.theme.MedicareappTheme
import pro.osin.tools.medicare.util.LocaleHelper
import pro.osin.tools.medicare.util.NotificationHelper
import pro.osin.tools.medicare.util.PreferencesManager
import pro.osin.tools.medicare.util.ReminderScheduler

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
            
            // Check and request notification permission
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Permission granted or denied - continue with app initialization
            }
            
            // Check and request notification policy access (Do Not Disturb)
            val notificationPolicyLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                // User returned from settings - continue with app initialization
            }
            
            // Check notification permission on startup
            LaunchedEffect(Unit) {
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
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        notificationPolicyLauncher.launch(intent)
                    }
                }
                
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
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
