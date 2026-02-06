package com.opentube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.opentube.data.local.dataStore
import com.opentube.ui.screens.settings.ThemeMode
import com.opentube.ui.theme.OpenTubeTheme
import com.opentube.ui.navigation.OpenTubeNavHost
import dagger.hilt.android.AndroidEntryPoint
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Ensure content is drawn behind system bars globally to fix padding issues
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            val context = LocalContext.current
            val themeMode by context.dataStore.data
                .map { preferences ->
                    preferences[stringPreferencesKey("theme")]?.let { themeName ->
                        ThemeMode.valueOf(themeName) 
                    } ?: ThemeMode.DARK
                }
                .collectAsState(initial = ThemeMode.DARK)
                
            val materialYouEnabled by context.dataStore.data
                .map { preferences ->
                    preferences[androidx.datastore.preferences.core.booleanPreferencesKey("material_you_enabled")] ?: false
                }
                .collectAsState(initial = false)
            
            OpenTubeTheme(
                themeMode = themeMode,
                materialYouEnabled = materialYouEnabled
            ) {
                // Este Surface es necesario para que el fondo no sea negro/transparente al usar EdgeToEdge
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpenTubeNavHost()
                }
            }
        }
    }
}