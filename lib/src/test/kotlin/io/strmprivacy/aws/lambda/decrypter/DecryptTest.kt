package io.strmprivacy.aws.lambda.decrypter

import com.google.crypto.tink.*
import com.google.crypto.tink.daead.DeterministicAeadConfig
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.util.*
import kotlin.system.exitProcess

class DecryptTest {
    private val encoder = Base64.getEncoder()
    private var encryptionKey: String

    init {
        try {
            DeterministicAeadConfig.register()
            encryptionKey = randomEncryptionKey()
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
            exitProcess(10)
        }
    }

    @Test
    fun `test happy flow decrypt`() {
        val plainText = "piet"
        val cipherText = encrypt(encryptionKey, plainText)
        val readBack = Decrypt.decrypt(encryptionKey, cipherText)
        readBack shouldBe plainText
    }

    @Test
    fun `test bad ciphertext`() {
        val cipherText = "this is not base64"
        assertThrows<BadCipherText> {
            Decrypt.decrypt(encryptionKey, cipherText)
        }
    }

    @Test
    fun `test bad ciphertext 2`() {
        val cipherText = encoder.encode("this is not a cipher".toByteArray()).decodeToString()
        assertThrows<BadCipherText> {
            Decrypt.decrypt(encryptionKey, cipherText)
        }
    }

    @Test
    fun `test bad ciphertext (wrong key)`() {
        val cipherText = "AUr7zSCRjFQiQMxc5WNeN4BUFRnKXtPAXIY="
        assertThrows<BadCipherText> {
            Decrypt.decrypt(encryptionKey, cipherText)
        }
    }

    @Test
    fun `test bad ciphertext 3`() {
        Decrypt.decrypt(encryptionKey, null) shouldBe null
    }

    @Test
    fun `test bad encryptionkey`() {
        assertThrows<BadEncryptionKey> {
            Decrypt.decrypt("piet", "")
        }
    }

    @Test
    fun `test bad encryptionkey 2`() {
        assertThrows<BadEncryptionKey> {
            Decrypt.decrypt("""{"validJson": true} """, "")
        }
    }

    fun encrypt(encryptionKey: String, plainText: String) = try {
        val daead = Decrypt.deterministicAead(encryptionKey)
        val ciphertext = daead!!.encryptDeterministically(plainText.toByteArray(), "".toByteArray())
        String(encoder.encode(ciphertext), StandardCharsets.UTF_8)
    } catch (e: GeneralSecurityException) {
        throw java.lang.RuntimeException(e.message)
    }

    fun randomEncryptionKey(): String = try {
        val keysetHandle = KeysetHandle.generateNew(KeyTemplates.get("AES256_SIV"))
        val bos = ByteArrayOutputStream()
        CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(bos))
        bos.toString(StandardCharsets.UTF_8)
    } catch (e: IOException) {
        throw RuntimeException(e.message)
    } catch (e: GeneralSecurityException) {
        throw RuntimeException(e.message)
    }
}
