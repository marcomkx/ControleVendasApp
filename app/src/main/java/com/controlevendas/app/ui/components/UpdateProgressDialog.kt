package com.controlevendas.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun UpdateProgressDialog(
    versionName: String,
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    onCancel: () -> Unit
) {
    val progressPercent = if (progress >= 0f) (progress * 100).toInt() else 0

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Baixando Atualização v$versionName", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$progressPercent%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val downloadedMb = downloadedBytes / (1024f * 1024f)
                        val totalMb = if (totalBytes > 0) totalBytes / (1024f * 1024f) else 0f
                        if (totalBytes > 0) {
                            Text(String.format(Locale("pt", "BR"), "%.1f MB / %.1f MB", downloadedMb, totalMb), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(String.format(Locale("pt", "BR"), "%.1f MB", downloadedMb), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp))
                    Text("Baixando...", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    )
}
