package com.isora.ciclo.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** The file is not a Ciclo backup, is truncated, or its content is invalid. */
class BackupFormatException(message: String) : Exception(message)

/** Wrong password, or the file was modified: AES-GCM cannot tell the two apart. */
class BackupPasswordException : Exception("Wrong password or damaged backup")

/**
 * Password-protected container of a backup, independent of the Keystore key of the phone so it can be
 * restored on another one. Layout: magic (8) | PBKDF2 iterations (4) | salt (16) | IV (12) | AES-256-GCM
 * ciphertext. The whole header is authenticated as associated data, so changing it breaks decryption.
 * Changing this layout is a format change: old backups must keep opening.
 */
object BackupCrypto {
    const val MIN_PASSWORD_LENGTH = 8

    private val MAGIC = "CICLOBK1".toByteArray(Charsets.US_ASCII)
    private const val ITERATIONS = 600_000
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 5_000_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private val HEADER_BYTES = MAGIC.size + 4 + SALT_BYTES + IV_BYTES

    private val random = SecureRandom()

    fun seal(plain: ByteArray, password: String): ByteArray {
        require(password.length >= MIN_PASSWORD_LENGTH) { "The password needs at least $MIN_PASSWORD_LENGTH characters" }
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).putInt(ITERATIONS).put(salt).put(iv).array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt, ITERATIONS), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    /** @throws BackupFormatException not a backup; @throws BackupPasswordException wrong password or tampered. */
    fun open(file: ByteArray, password: String): ByteArray {
        if (file.size < HEADER_BYTES + TAG_BITS / 8 || !file.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupFormatException("Not a Ciclo backup")
        }
        val buffer = ByteBuffer.wrap(file, MAGIC.size, file.size - MAGIC.size)
        val iterations = buffer.getInt()
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw BackupFormatException("Not a Ciclo backup")
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(file, 0, HEADER_BYTES)
        return try {
            cipher.doFinal(file, HEADER_BYTES, file.size - HEADER_BYTES)
        } catch (e: GeneralSecurityException) {
            throw BackupPasswordException()
        }
    }

    private fun key(password: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
