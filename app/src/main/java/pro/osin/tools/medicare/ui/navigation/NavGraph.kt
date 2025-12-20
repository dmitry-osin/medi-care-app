package pro.osin.tools.medicare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import pro.osin.tools.medicare.ui.screens.about.AboutScreen
import pro.osin.tools.medicare.ui.screens.home.HomeScreen
import pro.osin.tools.medicare.ui.screens.medicines.MedicinesListScreen
import pro.osin.tools.medicare.ui.screens.medicineform.MedicineFormScreen
import pro.osin.tools.medicare.ui.screens.reminders.RemindersListScreen
import pro.osin.tools.medicare.ui.screens.reminderform.ReminderFormScreen
import pro.osin.tools.medicare.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Medicines : Screen("medicines")
    object Reminders : Screen("reminders")
    object MedicineNew : Screen("medicine/new")
    object MedicineEdit : Screen("medicine/{id}") {
        fun createRoute(id: Long) = "medicine/$id"
    }
    object ReminderNew : Screen("reminder/new")
    object ReminderEdit : Screen("reminder/{id}") {
        fun createRoute(id: Long) = "reminder/$id"
    }
    object Settings : Screen("settings")
    object About : Screen("about")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Medicines.route) {
            MedicinesListScreen(navController = navController)
        }
        composable(Screen.Reminders.route) {
            RemindersListScreen(navController = navController)
        }
        composable(Screen.MedicineNew.route) {
            MedicineFormScreen(
                navController = navController,
                medicineId = null
            )
        }
        composable(Screen.MedicineEdit.route) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            MedicineFormScreen(
                navController = navController,
                medicineId = medicineId
            )
        }
        composable(Screen.ReminderNew.route) {
            ReminderFormScreen(
                navController = navController,
                reminderId = null
            )
        }
        composable(Screen.ReminderEdit.route) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            ReminderFormScreen(
                navController = navController,
                reminderId = reminderId
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.About.route) {
            AboutScreen(navController = navController)
        }
    }
}

