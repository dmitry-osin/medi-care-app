package pro.osin.tools.medicare.ui.screens.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersListScreen(navController: NavController) {
    val context = LocalContext.current
    
    // Cache database and repositories to avoid recreating on recomposition
    val database = remember { MedicineDatabase.getDatabase(context.applicationContext) }
    val reminderRepository = remember { ReminderRepository(database.reminderDao(), context.applicationContext) }
    val medicineRepository = remember { MedicineRepository(database.medicineDao()) }
    val scope = rememberCoroutineScope()
    
    val reminders = reminderRepository.getAllReminders().collectAsState(initial = emptyList())
    val activeMedicines = medicineRepository.getAllActiveMedicines().collectAsState(initial = emptyList())
    val allMedicines = medicineRepository.getAllMedicines().collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf<Reminder?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_reminders),
                navController = navController
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeMedicines.value.isEmpty()) {
                        // Navigate to create medicine if no medicines exist
                        navController.navigate(Screen.MedicineNew.route)
                    } else {
                        navController.navigate(Screen.ReminderNew.route)
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.reminder_new))
            }
        }
    ) { paddingValues ->
        if (reminders.value.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.reminders_no_reminders),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.reminders_add_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reminders.value) { reminder ->
                    val medicine = allMedicines.value.find { it.id == reminder.medicineId }
                    ReminderListItem(
                        reminder = reminder,
                        medicine = medicine,
                        onEditClick = { navController.navigate(Screen.ReminderEdit.createRoute(reminder.id)) },
                        onDeleteClick = { showDeleteDialog = reminder },
                        onToggleActive = { isActive ->
                            scope.launch {
                                // Check if trying to activate reminder for inactive medicine
                                if (isActive && medicine != null && !medicine.isActive) {
                                    // Cannot activate reminder for inactive medicine
                                    return@launch
                                }
                                reminderRepository.updateReminder(reminder.copy(isActive = isActive))
                            }
                        }
                    )
                }
            }
        }
    }

    showDeleteDialog?.let { reminder ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.reminder_delete_confirmation)) },
            text = { Text(stringResource(R.string.medicine_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            reminderRepository.deleteReminder(reminder)
                            showDeleteDialog = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ReminderListItem(
    reminder: Reminder,
    medicine: Medicine?,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    val days = remember(reminder) {
        buildList {
            if (reminder.monday) add(DayOfWeek.MONDAY)
            if (reminder.tuesday) add(DayOfWeek.TUESDAY)
            if (reminder.wednesday) add(DayOfWeek.WEDNESDAY)
            if (reminder.thursday) add(DayOfWeek.THURSDAY)
            if (reminder.friday) add(DayOfWeek.FRIDAY)
            if (reminder.saturday) add(DayOfWeek.SATURDAY)
            if (reminder.sunday) add(DayOfWeek.SUNDAY)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = reminder.time,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        days.forEach { day ->
                            Text(
                                text = day.getShortName(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (day.isWeekend) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = onToggleActive,
                    enabled = medicine?.isActive != false
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}
