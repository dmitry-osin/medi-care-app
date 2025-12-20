package pro.osin.tools.medicare.ui.screens.reminderform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import pro.osin.tools.medicare.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFormScreen(navController: NavController, reminderId: Long?) {
    val context = LocalContext.current
    val database = MedicineDatabase.getDatabase(context)
    val medicineRepository = MedicineRepository(database.medicineDao())
    val reminderRepository = ReminderRepository(database.reminderDao(), context)
    val scope = rememberCoroutineScope()

    var reminder by remember { mutableStateOf<Reminder?>(null) }
    var selectedMedicine by remember { mutableStateOf<Medicine?>(null) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) } // hour, minute
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }

    var showMedicineMenu by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val medicines = medicineRepository.getAllActiveMedicines().collectAsState(initial = emptyList())

    // Load reminder if editing, or set current time for new reminder
    LaunchedEffect(reminderId) {
        if (reminderId != null) {
            reminderRepository.getReminderById(reminderId)?.let { rem ->
                reminder = rem
                medicineRepository.getMedicineById(rem.medicineId)?.let { med ->
                    // Only set selected medicine if it's active
                    if (med.isActive) {
                        selectedMedicine = med
                    } else {
                        // Medicine is inactive, clear selection
                        selectedMedicine = null
                    }
                }
                val timeParts = rem.time.split(":")
                selectedTime = Pair(timeParts[0].toInt(), timeParts[1].toInt())
                
                val days = mutableSetOf<DayOfWeek>()
                if (rem.monday) days.add(DayOfWeek.MONDAY)
                if (rem.tuesday) days.add(DayOfWeek.TUESDAY)
                if (rem.wednesday) days.add(DayOfWeek.WEDNESDAY)
                if (rem.thursday) days.add(DayOfWeek.THURSDAY)
                if (rem.friday) days.add(DayOfWeek.FRIDAY)
                if (rem.saturday) days.add(DayOfWeek.SATURDAY)
                if (rem.sunday) days.add(DayOfWeek.SUNDAY)
                selectedDays = days
            }
        } else {
            // Set current time for new reminder
            val calendar = java.util.Calendar.getInstance()
            selectedTime = Pair(calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (reminderId == null) R.string.reminder_new else R.string.reminder_edit)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (selectedMedicine == null) {
                                errorMessage = context.getString(R.string.required_field)
                                return@TextButton
                            }
                            if (selectedTime == null) {
                                errorMessage = context.getString(R.string.required_field)
                                return@TextButton
                            }
                            if (selectedDays.isEmpty()) {
                                errorMessage = context.getString(R.string.reminder_no_days_error)
                                return@TextButton
                            }

                            // Check if medicine is active
                            if (selectedMedicine != null && !selectedMedicine!!.isActive) {
                                errorMessage = context.getString(R.string.reminder_medicine_inactive_error)
                                return@TextButton
                            }

                            // Check maximum reminder count
                            scope.launch {
                                val count = reminderRepository.getActiveReminderCountForMedicine(selectedMedicine!!.id)
                                if (count >= 5 && reminderId == null) {
                                    errorMessage = context.getString(R.string.reminder_max_count_error)
                                    return@launch
                                }

                                val timeString = String.format("%02d:%02d", selectedTime!!.first, selectedTime!!.second)
                                
                                val reminderToSave = Reminder(
                                    id = reminder?.id ?: 0,
                                    medicineId = selectedMedicine!!.id,
                                    time = timeString,
                                    monday = selectedDays.contains(DayOfWeek.MONDAY),
                                    tuesday = selectedDays.contains(DayOfWeek.TUESDAY),
                                    wednesday = selectedDays.contains(DayOfWeek.WEDNESDAY),
                                    thursday = selectedDays.contains(DayOfWeek.THURSDAY),
                                    friday = selectedDays.contains(DayOfWeek.FRIDAY),
                                    saturday = selectedDays.contains(DayOfWeek.SATURDAY),
                                    sunday = selectedDays.contains(DayOfWeek.SUNDAY),
                                    isActive = if (selectedMedicine != null && !selectedMedicine!!.isActive) false else (reminder?.isActive ?: true)
                                )

                                if (reminderId == null) {
                                    reminderRepository.insertReminder(reminderToSave)
                                } else {
                                    reminderRepository.updateReminder(reminderToSave)
                                }
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                LaunchedEffect(it) {
                    kotlinx.coroutines.delay(3000)
                    errorMessage = null
                }
            }

            // Show message if no medicines available
            if (reminderId == null && medicines.value.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reminders_no_medicines_error),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = {
                                navController.popBackStack()
                                navController.navigate(Screen.MedicineNew.route)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.medicine_new))
                        }
                    }
                }
            }

            // Medicine selection
            ExposedDropdownMenuBox(
                expanded = showMedicineMenu,
                onExpandedChange = { showMedicineMenu = !showMedicineMenu }
            ) {
                OutlinedTextField(
                    value = selectedMedicine?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.reminder_medicine)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMedicineMenu) },
                    isError = selectedMedicine == null
                )
                ExposedDropdownMenu(
                    expanded = showMedicineMenu,
                    onDismissRequest = { showMedicineMenu = false }
                ) {
                    // Only show active medicines
                    medicines.value.filter { it.isActive }.forEach { medicine ->
                        DropdownMenuItem(
                            text = { Text(medicine.name) },
                            onClick = {
                                selectedMedicine = medicine
                                showMedicineMenu = false
                            }
                        )
                    }
                }
            }

            // Time
            OutlinedTextField(
                value = selectedTime?.let { String.format("%02d:%02d", it.first, it.second) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.reminder_time)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Text("🕐")
                    }
                },
                isError = selectedTime == null
            )

            if (showTimePicker) {
                val calendar = java.util.Calendar.getInstance()
                val timePickerState = rememberTimePickerState(
                    initialHour = selectedTime?.first ?: calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    initialMinute = selectedTime?.second ?: calendar.get(java.util.Calendar.MINUTE)
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedTime = Pair(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) {
                            Text(stringResource(R.string.apply))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    }
                )
            }

            // Days of week
            Text(
                text = stringResource(R.string.reminder_days),
                style = MaterialTheme.typography.titleMedium
            )
            
            DayOfWeek.values().forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = selectedDays.contains(day),
                            onCheckedChange = { checked ->
                                selectedDays = if (checked) {
                                    selectedDays + day
                                } else {
                                    selectedDays - day
                                }
                            }
                        )
                        Text(
                            text = day.getFullName(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (day.isWeekend) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
