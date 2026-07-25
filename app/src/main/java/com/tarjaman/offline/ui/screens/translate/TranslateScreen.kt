package com.tarjaman.offline.ui.screens.translate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tarjaman.offline.R
import com.tarjaman.offline.ui.components.ModelStatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(viewModel: TranslateViewModel) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // شريط اختيار اللغتين مع زر التبديل
            LanguageSwitchBar(
                sourceLang = state.sourceLang,
                targetLang = state.targetLang,
                onSwap = viewModel::swapLanguages
            )

            // مربع النص المصدر
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModelStatusChip(
                            state = state.sourceModelState,
                            onDownloadClick = { viewModel.downloadModel(state.sourceLang, wifiOnly = true) }
                        )
                        if (state.inputText.isNotEmpty()) {
                            IconButton(onClick = viewModel::clearInput) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.btn_clear))
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = viewModel::onInputChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        placeholder = { Text(stringResource(R.string.hint_enter_text)) },
                        minLines = 4,
                        maxLines = 8
                    )
                }
            }

            // مربع نتيجة الترجمة
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModelStatusChip(
                            state = state.targetModelState,
                            onDownloadClick = { viewModel.downloadModel(state.targetLang, wifiOnly = true) }
                        )
                        if (state.isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }

                    Text(
                        text = state.outputText.ifBlank { stringResource(R.string.translation_placeholder) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        color = if (state.outputText.isBlank())
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    AnimatedVisibility(visible = state.outputText.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(state.outputText))
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.btn_copy))
                            }
                            IconButton(onClick = {
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, state.outputText)
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.btn_share))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSwitchBar(sourceLang: String, targetLang: String, onSwap: () -> Unit) {
    fun label(code: String) = if (code == "ar")
        androidx.compose.ui.res.stringResource(R.string.lang_arabic)
    else
        androidx.compose.ui.res.stringResource(R.string.lang_english)

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label(sourceLang),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            FilledIconButton(onClick = onSwap) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = stringResource(R.string.btn_swap_languages))
            }
            Text(
                text = label(targetLang),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
