package pro.osin.tools.medicare.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.combine
import pro.osin.tools.medicare.R
import pro.osin.tools.medicare.data.database.MedicineDatabase
import pro.osin.tools.medicare.data.database.entities.Medicine
import pro.osin.tools.medicare.data.database.entities.Reminder
import pro.osin.tools.medicare.data.repository.MedicineRepository
import pro.osin.tools.medicare.data.repository.ReminderRepository
import pro.osin.tools.medicare.domain.model.DayOfWeek
import pro.osin.tools.medicare.ui.components.AppTopBar
import pro.osin.tools.medicare.ui.components.BottomNavigationBar
import pro.osin.tools.medicare.ui.navigation.Screen
import java.util.*

// Data class for reminder items with medicine info
data class ReminderItem(
    val reminder: Reminder,
    val medicine: Medicine,
    val isPast: Boolean,
    val dayType: String // "today_past", "today_future", "tomorrow"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    
    // Cache database and repositories to avoid recreating on recomposition
    val database = remember { MedicineDatabase.getDatabase(context.applicationContext) }
    val medicineRepository = remember { MedicineRepository(database.medicineDao()) }
    val reminderRepository = remember { ReminderRepository(database.reminderDao(), context.applicationContext) }

    // Update current time periodically (every minute)
    val currentTimeStringState = remember { 
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        mutableStateOf(String.format("%02d:%02d", hour, minute))
    }
    
    // Cache calendar calculations - update when day changes
    // Use current time as dependency to update when day changes
    val currentTimeForDateKey = remember { 
        mutableStateOf(System.currentTimeMillis())
    }
    
    // Update date key when time updates (to catch day changes)
    LaunchedEffect(currentTimeStringState.value) {
        val currentMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        val previousCalendar = Calendar.getInstance().apply { timeInMillis = currentTimeForDateKey.value }
        val previousDay = previousCalendar.get(Calendar.DAY_OF_MONTH)
        
        if (currentDay != previousDay) {
            currentTimeForDateKey.value = currentMillis
        }
    }
    
    val currentDateKey = remember(currentTimeForDateKey.value) { 
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeForDateKey.value
        }
        calendar.get(Calendar.YEAR) * 10000 + 
        calendar.get(Calendar.MONTH) * 100 + 
        calendar.get(Calendar.DAY_OF_MONTH)
    }
    
    val (dayOfWeekIndex, tomorrowDayOfWeekIndex) = remember(currentDateKey) {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val todayIndex = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        
        val tomorrowCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        val tomorrowDayOfWeek = tomorrowCalendar.get(Calendar.DAY_OF_WEEK)
        val tomorrowIndex = if (tomorrowDayOfWeek == Calendar.SUNDAY) 7 else tomorrowDayOfWeek - 1
        
        Pair(todayIndex, tomorrowIndex)
    }
    
    LaunchedEffect(Unit) {
        try {
            while (true) {
                kotlinx.coroutines.delay(60000) // Update every minute
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                currentTimeStringState.value = String.format("%02d:%02d", hour, minute)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Normal cancellation when leaving composition
            throw e
        }
    }

    val allMedicines = medicineRepository.getAllMedicines().collectAsState(initial = emptyList())
    val remindersForToday = reminderRepository.getRemindersForDay(dayOfWeekIndex).collectAsState(initial = emptyList())
    val remindersForTomorrow = reminderRepository.getRemindersForDay(tomorrowDayOfWeekIndex).collectAsState(initial = emptyList())

    // Combine reminders with medicines and categorize
    val reminderItems = remember(
        remindersForToday.value,
        remindersForTomorrow.value,
        allMedicines.value,
        currentTimeStringState.value
    ) {
        val items = mutableListOf<ReminderItem>()
        
        // Process today's reminders
        remindersForToday.value.forEach { reminder ->
            allMedicines.value.find { it.id == reminder.medicineId && it.isActive }?.let { medicine ->
                val isPast = reminder.time < currentTimeStringState.value
                items.add(
                    ReminderItem(
                        reminder = reminder,
                        medicine = medicine,
                        isPast = isPast,
                        dayType = if (isPast) "today_past" else "today_future"
                    )
                )
            }
        }
        
        // Process tomorrow's reminders
        remindersForTomorrow.value.forEach { reminder ->
            allMedicines.value.find { it.id == reminder.medicineId && it.isActive }?.let { medicine ->
                items.add(
                    ReminderItem(
                        reminder = reminder,
                        medicine = medicine,
                        isPast = false,
                        dayType = "tomorrow"
                    )
                )
            }
        }
        
        // Sort: today_past (by time desc), today_future (by time asc), tomorrow (by time asc)
        val pastToday = items.filter { it.dayType == "today_past" }.sortedByDescending { it.reminder.time }
        val futureToday = items.filter { it.dayType == "today_future" }.sortedBy { it.reminder.time }
        val tomorrow = items.filter { it.dayType == "tomorrow" }.sortedBy { it.reminder.time }
        
        pastToday + futureToday + tomorrow
    }

    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.home_title),
                navController = navController
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            if (allMedicines.value.isEmpty()) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.MedicineNew.route) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add_medicine))
                }
            } else {
                if (expanded) {
                    Column(
                        modifier = Modifier.padding(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                expanded = false
                                navController.navigate(Screen.ReminderNew.route)
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.home_add_reminder))
                        }
                        FloatingActionButton(
                            onClick = {
                                expanded = false
                                navController.navigate(Screen.MedicineNew.route)
                            }
                        ) {
                            Icon(Icons.Default.List, contentDescription = stringResource(R.string.home_add_medicine))
                        }
                    }
                } else {
                    FloatingActionButton(
                        onClick = { expanded = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add_medicine))
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(
                    if (expanded) {
                        Modifier.clickable { expanded = false }
                    } else {
                        Modifier
                    }
                )
        ) {
            if (reminderItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_no_reminders),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.home_add_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var lastDayType: String? = null
                    var todayHeaderShown = false
                    
                    reminderItems.forEach { item ->
                        // Add divider for day type change
                        if (lastDayType != item.dayType) {
                            when (item.dayType) {
                                "today_past", "today_future" -> {
                                    if (!todayHeaderShown) {
                                        item {
                                            Text(
                                                text = stringResource(R.string.home_today),
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        todayHeaderShown = true
                                    }
                                }
                                "tomorrow" -> {
                                    item {
                                        Text(
                                            text = stringResource(R.string.home_tomorrow),
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            lastDayType = item.dayType
                        }
                        
                        item {
                            ReminderItemCard(reminderItem = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderItemCard(
    reminderItem: ReminderItem
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(reminderItem.medicine.color),
                            CircleShape
                        )
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminderItem.medicine.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${reminderItem.medicine.dosage} - ${reminderItem.medicine.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = reminderItem.reminder.time,
                style = MaterialTheme.typography.titleLarge,
                color = if (reminderItem.isPast) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

