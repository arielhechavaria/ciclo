package com.isora.ciclo.settings

import com.isora.ciclo.ui.CicloSpacing
import com.isora.ciclo.ui.CicloTextField
import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.isora.ciclo.R
import com.isora.ciclo.backup.BackupSection
import com.isora.ciclo.backup.BackupService
import com.isora.ciclo.notifications.FridayNotifier
import com.isora.ciclo.security.LockGuard
import com.isora.ciclo.security.PinStore
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

/** Friday notification (switch, time, the permissions it needs on old phones) and the lock. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    pins: PinStore,
    backup: BackupService,
    lockGuard: LockGuard,
    onRestored: () -> Unit,
    onLockNow: () -> Unit,
    onLoadMock: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(settings.fridayEnabled) }
    var timeText by remember { mutableStateOf(settings.fridayTime.format(timeFormat)) }
    var timeError by remember { mutableStateOf(false) }
    var timeSaved by remember { mutableStateOf(false) }
    var fingerprint by remember { mutableStateOf(pins.fingerprintEnabled) }
    var newPin by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<Int?>(null) }
    var mockLoaded by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        FridayNotifier.schedule(context)
    }
    val fingerprintAvailable = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CicloSpacing.md)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        Text(stringResource(R.string.settings_friday), Modifier.padding(top = CicloSpacing.md), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.settings_friday_hint), style = MaterialTheme.typography.bodySmall)
        Row(
            Modifier.toggleable(value = enabled, role = Role.Switch, onValueChange = {
                enabled = it
                settings.fridayEnabled = it
                FridayNotifier.schedule(context)
            }).padding(vertical = CicloSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(checked = enabled, onCheckedChange = null)
            Text(stringResource(R.string.settings_friday_switch), Modifier.padding(start = CicloSpacing.xs))
        }
        CicloTextField(
            value = timeText,
            onValueChange = {
                timeText = it
                timeError = false
                timeSaved = false
            },
            label = { Text(stringResource(R.string.settings_time)) },
            isError = timeError,
            supportingText = if (timeError) {
                { Text(stringResource(R.string.settings_time_error)) }
            } else if (timeSaved) {
                { Text(stringResource(R.string.settings_time_saved)) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = {
                val parsed = AppSettings.parseTime(timeText)
                if (parsed == null) {
                    timeError = true
                } else {
                    settings.fridayTime = parsed
                    timeText = parsed.format(timeFormat)
                    timeSaved = true
                    FridayNotifier.schedule(context)
                }
            },
            modifier = Modifier.padding(top = CicloSpacing.xxs),
        ) { Text(stringResource(R.string.protocol_form_save)) }

        Text(stringResource(R.string.settings_reliability), Modifier.padding(top = CicloSpacing.xl), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.settings_reliability_hint), style = MaterialTheme.typography.bodySmall)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OutlinedButton(
                onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
            ) { Text(stringResource(R.string.settings_allow_notifications)) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            OutlinedButton(
                onClick = {
                    val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    if (!alarms.canScheduleExactAlarms()) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
            ) { Text(stringResource(R.string.settings_exact_alarm)) }
        }
        OutlinedButton(
            onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
            modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
        ) { Text(stringResource(R.string.settings_battery)) }

        BackupSection(backup, lockGuard, onRestored)

        Text(stringResource(R.string.settings_lock), Modifier.padding(top = CicloSpacing.xl), style = MaterialTheme.typography.titleMedium)
        if (fingerprintAvailable) {
            Row(
                Modifier.toggleable(value = fingerprint, role = Role.Switch, onValueChange = {
                    fingerprint = it
                    pins.fingerprintEnabled = it
                }).padding(vertical = CicloSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(checked = fingerprint, onCheckedChange = null)
                Text(stringResource(R.string.settings_fingerprint), Modifier.padding(start = CicloSpacing.xs))
            }
        }
        CicloTextField(
            value = newPin,
            onValueChange = {
                if (it.length <= PinStore.MAX_LENGTH && it.all(Char::isDigit)) newPin = it
                pinMessage = null
            },
            label = { Text(stringResource(R.string.settings_new_pin)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        pinMessage?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
        OutlinedButton(
            onClick = {
                if (PinStore.isValidFormat(newPin)) {
                    pins.set(newPin)
                    newPin = ""
                    pinMessage = R.string.settings_pin_changed
                } else {
                    pinMessage = R.string.lock_error_format
                }
            },
            modifier = Modifier.padding(top = CicloSpacing.xxs),
        ) { Text(stringResource(R.string.settings_change_pin)) }
        Button(onClick = onLockNow, Modifier.fillMaxWidth().padding(top = CicloSpacing.md)) {
            Text(stringResource(R.string.settings_lock_now))
        }

        if (onLoadMock != null) {
            Text(stringResource(R.string.settings_mock), Modifier.padding(top = CicloSpacing.xl), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_mock_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = {
                    onLoadMock()
                    mockLoaded = true
                },
                modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
            ) { Text(stringResource(R.string.settings_mock_load)) }
            if (mockLoaded) Text(stringResource(R.string.settings_mock_loaded), style = MaterialTheme.typography.bodySmall)
        }
    }
}
