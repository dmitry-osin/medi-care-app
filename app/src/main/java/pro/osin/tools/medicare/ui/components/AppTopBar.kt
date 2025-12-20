package pro.osin.tools.medicare.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import pro.osin.tools.medicare.R
import pro.osin.tools.medicare.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    navController: NavController,
    showSettingsButton: Boolean = true
) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            if (showSettingsButton) {
                IconButton(
                    onClick = { navController.navigate(Screen.Settings.route) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.nav_settings)
                    )
                }
            }
        }
    )
}

