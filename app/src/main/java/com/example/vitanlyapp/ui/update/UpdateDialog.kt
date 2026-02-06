package com.example.vitanlyapp.ui.update

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Диалог обновления приложения.
 *
 * Показывается когда доступно новое обновление.
 * Позволяет скачать и установить APK.
 */
@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val state by viewModel.updateState.collectAsState()
    val progress by viewModel.downloadProgress.collectAsState()
    val context = LocalContext.current

    // Показываем диалог только для определённых состояний
    when (val currentState = state) {
        is UpdateUiState.UpdateAvailable -> {
            UpdateAvailableDialog(
                currentVersion = currentState.currentVersion,
                newVersion = currentState.newVersion,
                releaseNotes = currentState.release.body,
                apkSizeMb = currentState.release.apkSize / (1024f * 1024f),
                onDownload = {
                    currentState.release.apkDownloadUrl?.let { url ->
                        viewModel.startDownload(url)
                    }
                },
                onDismiss = {
                    viewModel.dismissUpdate()
                    onDismiss()
                }
            )
        }

        is UpdateUiState.Downloading -> {
            DownloadingDialog(
                progress = progress,
                onCancel = {
                    viewModel.resetState()
                    onDismiss()
                }
            )
        }

        is UpdateUiState.ReadyToInstall -> {
            ReadyToInstallDialog(
                onInstall = {
                    if (viewModel.canInstallFromUnknownSources()) {
                        viewModel.getInstallIntent(currentState.apkPath)?.let { intent ->
                            context.startActivity(intent)
                        }
                    } else {
                        // Открываем настройки для разрешения установки
                        context.startActivity(viewModel.getUnknownSourcesSettingsIntent())
                    }
                },
                onDismiss = {
                    viewModel.dismissUpdate()
                    onDismiss()
                },
                needsPermission = !viewModel.canInstallFromUnknownSources()
            )
        }

        is UpdateUiState.Error -> {
            ErrorDialog(
                message = currentState.message,
                onRetry = { viewModel.checkForUpdates() },
                onDismiss = {
                    viewModel.dismissUpdate()
                    onDismiss()
                }
            )
        }

        else -> {
            // Idle, Checking, UpToDate, Dismissed — не показываем диалог
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    currentVersion: String,
    newVersion: String,
    releaseNotes: String,
    apkSizeMb: Float,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Доступно обновление",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$currentVersion → $newVersion",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                if (releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = releaseNotes.take(200) + if (releaseNotes.length > 200) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Размер: %.1f МБ".format(apkSizeMb),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Позже")
                    }

                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Скачать")
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadingDialog(
    progress: Float,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = { /* Не позволяем закрыть во время загрузки */ }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Загрузка обновления",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = onCancel) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
private fun ReadyToInstallDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    needsPermission: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (needsPermission) "Требуется разрешение" else "Готово к установке",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (needsPermission)
                        "Разрешите установку из неизвестных источников для этого приложения"
                    else
                        "Обновление скачано. Нажмите «Установить» чтобы продолжить.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Позже")
                    }

                    Button(
                        onClick = onInstall,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (needsPermission) Icons.Default.Settings else Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (needsPermission) "Настройки" else "Установить")
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ошибка",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Закрыть")
                    }

                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Повторить")
                    }
                }
            }
        }
    }
}
