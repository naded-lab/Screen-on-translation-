package com.tarjaman.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tarjaman.offline.data.prefs.ThemeMode
import com.tarjaman.offline.ui.font.AppFont
import com.tarjaman.offline.ui.navigation.TarjamanNavHost
import com.tarjaman.offline.ui.theme.TarjamanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TarjamanApp).container

        setContent {
            val themeMode by container.preferencesManager.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val fontKey by container.preferencesManager.fontKeyFlow.collectAsState(initial = "system_default")
            val fontScale by container.preferencesManager.fontScaleFlow.collectAsState(initial = 1.0f)

            TarjamanTheme(
                themeMode = themeMode,
                appFont = AppFont.fromKey(fontKey),
                fontScale = fontScale
            ) {
                TarjamanNavHost(container = container)
            }
        }
    }
}
