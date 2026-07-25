package com.tarjaman.offline.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tarjaman.offline.R
import com.tarjaman.offline.data.translate.ModelState

/**
 * مؤشر حالة نموذج الترجمة. يعطي المستخدم دائماً معرفة واضحة ما إذا كانت
 * الترجمة لهذه اللغة جاهزة Offline أم تحتاج تنزيلاً أولاً.
 */
@Composable
fun ModelStatusChip(
    state: ModelState,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(targetState = state, label = "model_status") { current ->
        when (current) {
            is ModelState.Ready -> AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(stringResource(R.string.model_status_ready)) },
                leadingIcon = {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    disabledLabelColor = MaterialTheme.colorScheme.primary,
                    disabledLeadingIconContentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = modifier
            )

            is ModelState.Downloading -> AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(stringResource(R.string.model_status_installing)) },
                leadingIcon = {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                },
                modifier = modifier
            )

            is ModelState.NotDownloaded, is ModelState.Error -> AssistChip(
                onClick = onDownloadClick,
                label = { Text(stringResource(R.string.btn_download_model)) },
                leadingIcon = {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                shape = RoundedCornerShape(50),
                modifier = modifier
            )
        }
    }
}
