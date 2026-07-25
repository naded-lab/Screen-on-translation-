package com.tarjaman.offline.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tarjaman.offline.R
import com.tarjaman.offline.di.AppContainer
import com.tarjaman.offline.di.ViewModelFactory
import com.tarjaman.offline.ui.screens.history.HistoryScreen
import com.tarjaman.offline.ui.screens.history.HistoryViewModel
import com.tarjaman.offline.ui.screens.overlay.OverlayTranslatorScreen
import com.tarjaman.offline.ui.screens.overlay.OverlayViewModel
import com.tarjaman.offline.ui.screens.settings.SettingsScreen
import com.tarjaman.offline.ui.screens.settings.SettingsViewModel
import com.tarjaman.offline.ui.screens.translate.TranslateScreen
import com.tarjaman.offline.ui.screens.translate.TranslateViewModel

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Destinations.TRANSLATE, R.string.nav_translate, Icons.Filled.Translate),
    BottomTab(Destinations.OVERLAY, R.string.nav_overlay, Icons.Filled.PictureInPicture),
    BottomTab(Destinations.HISTORY, R.string.nav_history, Icons.Filled.History),
    BottomTab(Destinations.SETTINGS, R.string.nav_settings, Icons.Filled.Settings)
)

@Composable
fun TarjamanNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val factory = ViewModelFactory(container)

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.TRANSLATE,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) }
        ) {
            composable(Destinations.TRANSLATE) {
                val vm: TranslateViewModel = viewModel(factory = factory)
                TranslateScreen(vm)
            }
            composable(Destinations.OVERLAY) {
                val vm: OverlayViewModel = viewModel()
                // زوج اللغة ثابت عمداً: إنجليزي -> عربي، لأن معظم فصول المانهوا/الويبتون
                // المعروضة في تطبيقات القراءة (مثل Mihon) تكون بالإنجليزية أصلاً
                OverlayTranslatorScreen(vm, sourceLang = "en", targetLang = "ar")
            }
            composable(Destinations.HISTORY) {
                val vm: HistoryViewModel = viewModel(factory = factory)
                HistoryScreen(vm)
            }
            composable(Destinations.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(vm)
            }
        }
    }
}
