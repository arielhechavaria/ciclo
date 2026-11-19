package com.isora.ciclo.backup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import com.isora.ciclo.R
import com.isora.ciclo.security.LockGuard
import com.isora.ciclo.ui.CicloSpacing
import com.isora.ciclo.ui.CicloTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate

private const val MAX_BACKUP_BYTES = 32 * 1024 * 1024

private sealed interface Pending {
    /** Export: the password is typed twice, then the destination is picked. */
    data object Export : Pending

    /** Restore: the file is already read, the password and the confirmation come next. */
    class Restore(val file: ByteArray) : Pending
}

/** Settings block to save the data to a password-protected file and to restore it. */
@Composable
fun BackupSection(service: BackupService, lockGuard: LockGuard, onRestored: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Pending?>(null) }
    var exportPassword by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        lockGuard.suspended = false
        val password = exportPassword
        exportPassword = null
        if (uri == null || password == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            message = try {
                withContext(Dispatchers.IO) { write(context, uri, service.export(password)) }
                context.getString(R.string.backup_exported)
            } catch (e: Exception) {
                context.getString(R.string.backup_error_io)
            } finally {
                busy = false
            }
        }
    }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        lockGuard.suspended = false
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                pending = Pending.Restore(withContext(Dispatchers.IO) { read(context, uri) })
            } catch (e: BackupFormatException) {
                message = context.getString(R.string.backup_error_format)
            } catch (e: Exception) {
                message = context.getString(R.string.backup_error_io)
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_backup), Modifier.padding(top = CicloSpacing.xl), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.settings_backup_hint), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = { message = null; pending = Pending.Export },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
        ) { Text(stringResource(R.string.backup_export)) }
        OutlinedButton(
            onClick = {
                message = null
                lockGuard.suspended = true
                opener.launch(arrayOf("*/*"))
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
        ) { Text(stringResource(R.string.backup_restore)) }
        message?.let { Text(it, Modifier.padding(top = CicloSpacing.xs), style = MaterialTheme.typography.bodySmall) }
    }

    when (val step = pending) {
        null -> Unit
        Pending.Export -> PasswordDialog(
            title = R.string.backup_export,
            warning = null,
            confirmWithRepeat = true,
            onCancel = { pending = null },
            onConfirm = { password ->
                pending = null
                exportPassword = password
                lockGuard.suspended = true
                picker.launch("ciclo-respaldo-${LocalDate.now()}.ciclobak")
            },
        )
        is Pending.Restore -> PasswordDialog(
            title = R.string.backup_restore,
            warning = R.string.backup_restore_warning,
            confirmWithRepeat = false,
            onCancel = { pending = null },
            onConfirm = { password ->
                pending = null
                busy = true
                scope.launch {
                    message = try {
                        val summary = withContext(Dispatchers.IO) { service.restore(step.file, password) }
                        onRestored()
                        context.getString(R.string.backup_restored, summary.patients, summary.treatments)
                    } catch (e: BackupPasswordException) {
                        context.getString(R.string.backup_error_password)
                    } catch (e: BackupFormatException) {
                        context.getString(R.string.backup_error_format)
                    } catch (e: Exception) {
                        context.getString(R.string.backup_error_io)
                    } finally {
                        busy = false
                    }
                }
            },
        )
    }
}

@Composable
private fun PasswordDialog(
    title: Int,
    warning: Int?,
    confirmWithRepeat: Boolean,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val tooShort = confirmWithRepeat && password.length < BackupCrypto.MIN_PASSWORD_LENGTH
    val mismatch = confirmWithRepeat && password != repeat
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(title)) },
        text = {
            Column {
                warning?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
                CicloTextField(
                    value = password,
                    onValueChange = { password = it; showError = false },
                    label = { Text(stringResource(R.string.backup_password)) },
                    isError = showError && tooShort,
                    supportingText = if (showError && tooShort) {
                        { Text(stringResource(R.string.backup_password_short, BackupCrypto.MIN_PASSWORD_LENGTH)) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
                )
                if (confirmWithRepeat) {
                    CicloTextField(
                        value = repeat,
                        onValueChange = { repeat = it; showError = false },
                        label = { Text(stringResource(R.string.backup_password_repeat)) },
                        isError = showError && mismatch,
                        supportingText = if (showError && mismatch) {
                            { Text(stringResource(R.string.backup_password_mismatch)) }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (tooShort || mismatch || password.isEmpty()) showError = true else onConfirm(password)
            }) { Text(stringResource(R.string.backup_continue)) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.protocol_form_cancel)) } },
    )
}

private fun write(context: Context, uri: Uri, bytes: ByteArray) {
    val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")
    out.use { it.write(bytes) }
}

private fun read(context: Context, uri: Uri): ByteArray {
    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
    input.use { stream ->
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        while (true) {
            val count = stream.read(chunk)
            if (count < 0) break
            if (out.size() + count > MAX_BACKUP_BYTES) throw BackupFormatException("Too large to be a backup")
            out.write(chunk, 0, count)
        }
        return out.toByteArray()
    }
}
