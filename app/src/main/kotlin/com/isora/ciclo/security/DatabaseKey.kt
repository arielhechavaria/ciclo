package com.isora.ciclo.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The passphrase of the encrypted database. It is random, generated once, and stored only wrapped
 * (AES-GCM) by a key that lives in the Android Keystore and never leaves it.
 */
object DatabaseKey {
    private const val ALIAS = "ciclo_db_wrap"
    private const val PREFS = "ciclo_security"
    private const val KEY_WRAPPED = "db_key_wrapped"
    private const val KEY_IV = "db_key_iv"

    fun passphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wrapped = prefs.getString(KEY_WRAPPED, null)
        val iv = prefs.getString(KEY_IV, null)
        if (wrapped != null && iv != null) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            return cipher.doFinal(Base64.decode(wrapped, Base64.NO_WRAP))
        }
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        val sealed = cipher.doFinal(fresh)
        prefs.edit()
            .putString(KEY_WRAPPED, Base64.encodeToString(sealed, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit()
        return fresh
    }

    private fun wrappingKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
