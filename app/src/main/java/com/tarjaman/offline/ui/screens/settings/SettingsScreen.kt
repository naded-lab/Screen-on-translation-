package com.tarjaman.offline.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tarjaman.offline.BuildConfig
import com.tarjaman.offline.R
import com.tarjaman.offline.data.prefs.ThemeMode
import com.tarjaman.offline.data.translate.ModelState
import com.tarjaman.offline.ui.components.ModelStatusChip
import com.tarjaman.offline.ui.components.TarjamanTopBar
import com.tarjaman.offline.ui.font.AppFont

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TarjamanTopBar(title = stringResource(R.string.settings_title)) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_language), icon = Icons.Filled.Language) {
                    LanguageOptionRow("ar", stringResource(R.string.lang_arabic), state.appLanguage, viewModel::setAppLanguage)
                    LanguageOptionRow("en", stringResource(R.string.lang_english), state.appLanguage, viewModel::setAppLanguage)
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_section_appearance), icon = Icons.Filled.DarkMode) {
                    ThemeOptionRow(ThemeMode.SYSTEM, stringResource(R.string.settings_follow_system), state.themeMode, viewModel::setThemeMode)
                    ThemeOptionRow(ThemeMode.LIGHT, "Light", state.themeMode, viewModel::setThemeMode)
                    ThemeOptionRow(ThemeMode.DARK, stringResource(R.string.settings_dark_mode), state.themeMode, viewModel::setThemeMode)
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_section_font), icon = Icons.Filled.TextFields) {
                    AppFont.entries.forEach { font ->
                        FontOptionRow(font, state.fontKey, viewModel::setFontKey)
                    }
                    Column(Modifier.padding(top = 8.dp)) {
                        Text(
                            stringResource(R.string.settings_font_size) + "  ${String.format("%.0f", state.fontScale * 100)}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = state.fontScale,
                            onValueChange = viewModel::setFontScale,
                            valueRange = 0.85f..1.4f,
                            steps = 10
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_section_models), icon = Icons.Filled.Translate) {
                    ModelRow(
                        label = stringResource(R.string.lang_arabic),
                        state = state.arabicModelState,
                        onDownload = { viewModel.downloadModel("ar") },
                        onDelete = { viewModel.deleteModel("ar") }
                    )
                    ModelRow(
                        label = stringResource(R.string.lang_english),
                        state = state.englishModelState,
                        onDownload = { viewModel.downloadModel("en") },
                        onDelete = { viewModel.deleteModel("en") }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                stringResource(R.string.settings_wifi_only),
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(checked = state.wifiOnlyDownload, onCheckedChange = viewModel::setWifiOnlyDownload)
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_about), icon = Icons.Filled.Info) {
                    Text(
                        stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Column(Modifier.padding(top = 12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(code: String, label: String, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = (code == selected), onClick = { onSelect(code) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = (code == selected), onClick = { onSelect(code) })
        Text(label, modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ThemeOptionRow(mode: ThemeMode, label: String, selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = (mode == selected), onClick = { onSelect(mode) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = (mode == selected), onClick = { onSelect(mode) })
        Text(label, modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FontOptionRow(font: AppFont, selectedKey: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = (font.key == selectedKey), onClick = { onSelect(font.key) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = (font.key == selectedKey), onClick = { onSelect(font.key) })
        Text(
            stringResource(font.displayNameRes),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = font.fontFamily)
        )
    }
}

@Composable
private fun ModelRow(label: String, state: ModelState, onDownload: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        ModelStatusChip(state = state, onDownloadClick = onDownload)
        if (state is ModelState.Ready) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.settings_delete_model))
            }
        }
    }
}
