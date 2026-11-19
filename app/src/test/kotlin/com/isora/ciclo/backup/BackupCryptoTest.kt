package com.isora.ciclo.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {
    private val plain = "pacientes y tratamientos".toByteArray()
    private val password = "clave-segura-1"

    @Test
    fun `a sealed backup opens with the same password`() {
        assertArrayEquals(plain, BackupCrypto.open(BackupCrypto.seal(plain, password), password))
    }

    @Test
    fun `the sealed file does not contain the plain text`() {
        val sealed = BackupCrypto.seal(plain, password)
        assertFalse(String(sealed, Charsets.ISO_8859_1).contains("pacientes"))
    }

    @Test
    fun `sealing twice gives different files`() {
        assertFalse(BackupCrypto.seal(plain, password).contentEquals(BackupCrypto.seal(plain, password)))
    }

    @Test
    fun `a wrong password is refused`() {
        val sealed = BackupCrypto.seal(plain, password)
        assertThrows(BackupPasswordException::class.java) { BackupCrypto.open(sealed, "otra-clave-12") }
    }

    @Test
    fun `a changed byte in the content or the header is refused`() {
        val sealed = BackupCrypto.seal(plain, password)
        listOf(sealed.size - 1, 14, 30).forEach { index ->
            val damaged = sealed.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }
            assertThrows(BackupPasswordException::class.java) { BackupCrypto.open(damaged, password) }
        }
    }

    @Test
    fun `a file that is not a backup is refused as a format error`() {
        assertThrows(BackupFormatException::class.java) { BackupCrypto.open("hola".toByteArray(), password) }
        assertThrows(BackupFormatException::class.java) { BackupCrypto.open(ByteArray(200), password) }
    }

    @Test
    fun `a truncated backup is refused`() {
        val sealed = BackupCrypto.seal(plain, password)
        assertThrows(BackupFormatException::class.java) { BackupCrypto.open(sealed.copyOf(20), password) }
        assertThrows(BackupPasswordException::class.java) { BackupCrypto.open(sealed.copyOf(sealed.size - 3), password) }
    }

    @Test
    fun `a password shorter than the minimum cannot seal`() {
        assertThrows(IllegalArgumentException::class.java) { BackupCrypto.seal(plain, "corta") }
    }
}
