package com.isora.ciclo.security

import com.isora.ciclo.ui.CicloSpacing
import com.isora.ciclo.ui.CicloTextField
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.isora.ciclo.R

/** Asks for the PIN (or creates one the first time) and calls [onUnlocked] when the user gets in. */
@Composable
fun LockScreen(pins: PinStore, onUnlocked: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity
    val creating = !pins.hasPin()
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    var failures by remember { mutableStateOf(0) }

    fun fingerprintAvailable(): Boolean = pins.fingerprintEnabled &&
        BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS

    fun promptFingerprint() {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onUnlocked()
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.lock_fingerprint_title))
                .setNegativeButtonText(activity.getString(R.string.lock_use_pin))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build(),
        )
    }

    LaunchedEffect(Unit) { if (!creating && fingerprintAvailable()) promptFingerprint() }

    fun submit() {
        if (creating) {
            when {
                !PinStore.isValidFormat(pin) -> error = R.string.lock_error_format
                pin != confirm -> error = R.string.lock_error_mismatch
                else -> {
                    pins.set(pin)
                    onUnlocked()
                }
            }
        } else if (pins.verify(pin)) {
            onUnlocked()
        } else {
            failures++
            pin = ""
            error = R.string.lock_error_wrong
        }
    }

    Column(
        Modifier.fillMaxSize().padding(CicloSpacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Text(
            stringResource(if (creating) R.string.lock_create_title else R.string.lock_enter_title),
            Modifier.padding(vertical = CicloSpacing.md),
        )
        CicloTextField(
            value = pin,
            onValueChange = { if (it.length <= PinStore.MAX_LENGTH && it.all(Char::isDigit)) pin = it; error = null },
            label = { Text(stringResource(R.string.lock_pin)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        if (creating) {
            CicloTextField(
                value = confirm,
                onValueChange = { if (it.length <= PinStore.MAX_LENGTH && it.all(Char::isDigit)) confirm = it; error = null },
                label = { Text(stringResource(R.string.lock_pin_confirm)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().padding(top = CicloSpacing.xs),
            )
            Text(stringResource(R.string.lock_no_recovery), Modifier.padding(top = CicloSpacing.xs), style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = CicloSpacing.xs)) }
        Button(onClick = ::submit, Modifier.fillMaxWidth().padding(top = CicloSpacing.md)) {
            Text(stringResource(if (creating) R.string.lock_create_button else R.string.lock_unlock))
        }
        if (!creating && fingerprintAvailable()) {
            OutlinedButton(onClick = ::promptFingerprint, Modifier.fillMaxWidth().padding(top = CicloSpacing.xs)) {
                Text(stringResource(R.string.lock_use_fingerprint))
            }
        }
    }
}
