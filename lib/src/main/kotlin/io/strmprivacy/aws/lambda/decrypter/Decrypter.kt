package io.strmprivacy.aws.lambda.decrypter

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.DeterministicAead
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.daead.DeterministicAeadConfig
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.security.GeneralSecurityException
import java.util.*
import kotlin.system.exitProcess

/**
 * object that uses Google Tink format symmetric encryption to decrypt a ciphertext.
 */
object Decrypt {
    private val decoder = Base64.getDecoder()

    private val cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .build<String, DeterministicAead>()
    init {
        try {
            DeterministicAeadConfig.register()
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
            exitProcess(10)
        }
    }

    fun decrypt(encryptionKey: String, cipherText: String?): String? {
        if (cipherText == null) return null
        val daead = cache.get(encryptionKey) { deterministicAead(it) }
        return try {
            daead!!.decryptDeterministically(decoder.decode(cipherText), "".toByteArray())
        } catch (e: IllegalArgumentException) {
            throw BadCipherText(e)
        } catch (e: GeneralSecurityException) {
            throw BadCipherText(e)
        }.decodeToString()
    }

    /**
     * turn an encryptionKey in json format into a DeterministicAead.
     */
    fun deterministicAead(encryptionKey: String): DeterministicAead? = try {
        CleartextKeysetHandle.read(JsonKeysetReader.withString(encryptionKey))
            .getPrimitive(DeterministicAead::class.java)
    } catch (e: GeneralSecurityException) {
        throw BadEncryptionKey(e)
    } catch (e: IOException) {
        throw BadEncryptionKey(e)
    }
}

open class DecrypterExceptions(exception: Throwable) : RuntimeException(exception.message)
class BadEncryptionKey(exception: Throwable) : DecrypterExceptions(exception)
class BadCipherText(exception: Throwable) : DecrypterExceptions(exception)
