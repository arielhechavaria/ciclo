package com.isora.ciclo.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** The unlock PIN, kept as a salted PBKDF2 hash. There is no recovery: a forgotten PIN means a reinstall. */
class PinStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ciclo_security", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    fun set(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash(pin, salt), Base64.NO_WRAP))
            .commit()
    }

    fun verify(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val expected = prefs.getString(KEY_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(expected, hash(pin, salt))
    }

    var fingerprintEnabled: Boolean
        get() = prefs.getBoolean(KEY_FINGERPRINT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_FINGERPRINT, value).apply()
        }

    private fun hash(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, 120_000, 256))
            .encoded

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 8
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_FINGERPRINT = "fingerprint"

        fun isValidFormat(pin: String) = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it.isDigit() }
    }
}
