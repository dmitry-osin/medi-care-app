package pro.osin.tools.medicare.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import pro.osin.tools.medicare.MainActivity
import pro.osin.tools.medicare.R
import pro.osin.tools.medicare.ui.navigation.Screen
import pro.osin.tools.medicare.util.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val preferencesManager = PreferencesManager(context)
    val scope = rememberCoroutineScope()
    
    val language by preferencesManager.language.collectAsState(initial = PreferencesManager.LANGUAGE_RU)
    val theme by preferencesManager.theme.collectAsState(initial = PreferencesManager.THEME_SYSTEM)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language selection
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = language == PreferencesManager.LANGUAGE_RU,
                    onClick = {
                        scope.launch {
                            preferencesManager.setLanguage(PreferencesManager.LANGUAGE_RU)
                            // Restart Activity to apply language
                            val activity = context as? Activity
                            activity?.let {
                                val intent = Intent(it, MainActivity::class.java)
                                intent.putExtra("navigate_to", Screen.Settings.route)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                it.startActivity(intent)
                                it.overridePendingTransition(0, 0)
                                it.finish()
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.settings_language_russian)) }
                )
                FilterChip(
                    selected = language == PreferencesManager.LANGUAGE_EN,
                    onClick = {
                        scope.launch {
                            preferencesManager.setLanguage(PreferencesManager.LANGUAGE_EN)
                            // Restart Activity to apply language
                            val activity = context as? Activity
                            activity?.let {
                                val intent = Intent(it, MainActivity::class.java)
                                intent.putExtra("navigate_to", Screen.Settings.route)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                it.startActivity(intent)
                                it.overridePendingTransition(0, 0)
                                it.finish()
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.settings_language_english)) }
                )
            }
            
            Divider()
            
            // Theme selection
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = theme == PreferencesManager.THEME_LIGHT,
                    onClick = {
                        scope.launch {
                            preferencesManager.setTheme(PreferencesManager.THEME_LIGHT)
                        }
                    },
                    label = { Text(stringResource(R.string.settings_theme_light)) }
                )
                FilterChip(
                    selected = theme == PreferencesManager.THEME_DARK,
                    onClick = {
                        scope.launch {
                            preferencesManager.setTheme(PreferencesManager.THEME_DARK)
                        }
                    },
                    label = { Text(stringResource(R.string.settings_theme_dark)) }
                )
                FilterChip(
                    selected = theme == PreferencesManager.THEME_SYSTEM,
                    onClick = {
                        scope.launch {
                            preferencesManager.setTheme(PreferencesManager.THEME_SYSTEM)
                        }
                    },
                    label = { Text(stringResource(R.string.settings_theme_system)) }
                )
            }
        }
    }
}

