package pro.osin.tools.medicare.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import pro.osin.tools.medicare.R
import pro.osin.tools.medicare.ui.navigation.Screen

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val items = listOf(
        NavigationItem(
            route = Screen.Home.route,
            icon = Icons.Default.Home,
            labelResId = R.string.nav_home
        ),
        NavigationItem(
            route = Screen.Medicines.route,
            icon = Icons.Default.List,
            labelResId = R.string.nav_medicines
        ),
        NavigationItem(
            route = Screen.Reminders.route,
            icon = Icons.Default.Notifications,
            labelResId = R.string.nav_reminders
        ),
        NavigationItem(
            route = Screen.About.route,
            icon = Icons.Default.Info,
            labelResId = R.string.nav_about
        )
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelResId)) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Clear navigation stack to root when navigating to main screens
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same screen in the stack
                            launchSingleTop = true
                            // Restore state when returning
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

data class NavigationItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelResId: Int
)

