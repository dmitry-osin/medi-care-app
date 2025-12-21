package pro.osin.tools.medicare.ui.screens.medicineform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import pro.osin.tools.medicare.R
import pro.osin.tools.medicare.data.database.MedicineDatabase
import pro.osin.tools.medicare.data.database.entities.Medicine
import pro.osin.tools.medicare.data.repository.MedicineRepository
import pro.osin.tools.medicare.domain.model.MedicineType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineFormScreen(navController: NavController, medicineId: Long?) {
    val context = LocalContext.current
    
    // Cache database and repository to avoid recreating on recomposition
    val database = remember { MedicineDatabase.getDatabase(context.applicationContext) }
    val medicineRepository = remember { MedicineRepository(database.medicineDao()) }
    val scope = rememberCoroutineScope()

    var medicine by remember { mutableStateOf<Medicine?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<MedicineType?>(null) }
    var dosage by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedColor by remember { mutableStateOf(Color(0xFF6200EE)) }
    var isActive by remember { mutableStateOf(true) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load medicine if editing
    LaunchedEffect(medicineId) {
        if (medicineId != null) {
            medicineRepository.getMedicineById(medicineId)?.let { med ->
                medicine = med
                name = med.name
                description = med.description ?: ""
                selectedType = MedicineType.values().find { it.name == med.type }
                dosage = med.dosage
                quantity = med.quantity
                startDate = med.startDate
                endDate = med.endDate
                selectedColor = Color(med.color)
                isActive = med.isActive
            }
        } else {
            startDate = System.currentTimeMillis()
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (medicineId == null) R.string.medicine_new else R.string.medicine_edit)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = context.getString(R.string.required_field)
                                return@TextButton
                            }
                            if (selectedType == null) {
                                errorMessage = context.getString(R.string.required_field)
                                return@TextButton
                            }
                            if (endDate != null && startDate != null && endDate!! < startDate!!) {
                                errorMessage = context.getString(R.string.invalid_date)
                                return@TextButton
                            }

                            scope.launch {
                                val medicineToSave = Medicine(
                                    id = medicine?.id ?: 0,
                                    name = name,
                                    description = description.ifBlank { null },
                                    type = selectedType!!.name,
                                    dosage = dosage,
                                    quantity = quantity,
                                    startDate = startDate ?: System.currentTimeMillis(),
                                    endDate = endDate,
                                    color = selectedColor.toArgb(),
                                    isActive = isActive
                                )

                                if (medicineId == null) {
                                    medicineRepository.insertMedicine(medicineToSave)
                                } else {
                                    medicineRepository.updateMedicine(medicineToSave)
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

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.medicine_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = name.isBlank()
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.medicine_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Medicine type
            ExposedDropdownMenuBox(
                expanded = showTypeMenu,
                onExpandedChange = { showTypeMenu = !showTypeMenu }
            ) {
                OutlinedTextField(
                    value = selectedType?.getDisplayName() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.medicine_type)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeMenu) },
                    isError = selectedType == null
                )
                ExposedDropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = { showTypeMenu = false }
                ) {
                    MedicineType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.getDisplayName()) },
                            onClick = {
                                selectedType = type
                                showTypeMenu = false
                            }
                        )
                    }
                }
            }

            // Dosage
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text(stringResource(R.string.medicine_dosage)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("10 мг") }
            )

            // Quantity
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text(stringResource(R.string.medicine_quantity)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("1") }
            )

            // Start date
            OutlinedTextField(
                value = startDate?.let { dateFormat.format(Date(it)) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.medicine_start_date)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Text("📅")
                    }
                }
            )

            if (showStartDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = startDate
                )
                AlertDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                startDate = it
                            }
                            showStartDatePicker = false
                        }) {
                            Text(stringResource(R.string.apply))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    text = {
                        DatePicker(state = datePickerState)
                    }
                )
            }

            // End date
            OutlinedTextField(
                value = endDate?.let { dateFormat.format(Date(it)) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.medicine_end_date)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Row {
                        if (endDate != null) {
                            IconButton(onClick = { endDate = null }) {
                                Text("✕")
                            }
                        }
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Text("📅")
                        }
                    }
                }
            )

            if (showEndDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = endDate ?: startDate
                )
                AlertDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                endDate = it
                            }
                            showEndDatePicker = false
                        }) {
                            Text(stringResource(R.string.apply))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    text = {
                        DatePicker(state = datePickerState)
                    }
                )
            }

            // Color
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.medicine_color),
                    style = MaterialTheme.typography.bodyLarge
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val colors = listOf(
                        Color(0xFF6200EE), // Purple
                        Color(0xFF03DAC6), // Teal
                        Color(0xFF018786), // Dark Teal
                        Color(0xFFB00020), // Red
                        Color(0xFF3700B3), // Dark Purple
                        Color(0xFF00C853), // Green
                        Color(0xFFFF6D00), // Orange
                        Color(0xFF2962FF), // Blue
                        Color(0xFFE91E63), // Pink
                        Color(0xFF795548), // Brown
                        Color(0xFF607D8B), // Blue Grey
                        Color(0xFF9C27B0)  // Deep Purple
                    )
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color, CircleShape)
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                    } else Modifier
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Active status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.medicine_active),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }
        }
    }
}
