package pro.osin.tools.medicare.ui.screens.medicines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pro.osin.tools.medicare.R
import pro.osin.tools.medicare.data.database.MedicineDatabase
import pro.osin.tools.medicare.data.database.entities.Medicine
import pro.osin.tools.medicare.data.repository.MedicineRepository
import pro.osin.tools.medicare.data.repository.ReminderRepository
import pro.osin.tools.medicare.domain.model.MedicineType
import pro.osin.tools.medicare.ui.components.AppTopBar
import pro.osin.tools.medicare.ui.components.BottomNavigationBar
import pro.osin.tools.medicare.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicinesListScreen(navController: NavController) {
    val context = LocalContext.current
    val database = MedicineDatabase.getDatabase(context)
    val medicineRepository = MedicineRepository(database.medicineDao())
    val reminderRepository = ReminderRepository(database.reminderDao(), context)
    val scope = rememberCoroutineScope()
    
    val medicines = medicineRepository.getAllMedicines().collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf<Medicine?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_medicines),
                navController = navController
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.MedicineNew.route) }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.medicine_new))
            }
        }
    ) { paddingValues ->
        if (medicines.value.isEmpty()) {
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
                        text = stringResource(R.string.medicines_no_medicines),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.medicines_add_hint),
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
                items(medicines.value) { medicine ->
                    MedicineListItem(
                        medicine = medicine,
                        onEditClick = { navController.navigate(Screen.MedicineEdit.createRoute(medicine.id)) },
                        onDeleteClick = { showDeleteDialog = medicine },
                        onToggleActive = { isActive ->
                            scope.launch {
                                medicineRepository.updateMedicine(medicine.copy(isActive = isActive))
                                
                                // If medicine is deactivated, deactivate all its reminders
                                if (!isActive) {
                                    reminderRepository.deactivateRemindersForMedicine(medicine.id)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    showDeleteDialog?.let { medicine ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.medicine_delete_confirmation)) },
            text = { Text(stringResource(R.string.medicine_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            medicineRepository.deleteMedicine(medicine)
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
fun MedicineListItem(
    medicine: Medicine,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    val medicineType = MedicineType.values().find { it.name == medicine.type }
    
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(medicine.color), CircleShape)
                            .padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = medicine.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = medicineType?.getDisplayName() ?: medicine.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${medicine.dosage} - ${medicine.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = medicine.isActive,
                    onCheckedChange = onToggleActive
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
