package io.strmprivacy.aws.lambda.decrypter

import com.google.gson.GsonBuilder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UdfTest {
    val decryptTest = DecryptTest()
    val encryptionKey = decryptTest.randomEncryptionKey()
    val gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    @Test
    fun `test happy flow 1`() {
        val handler = Handler()

        val cipherText1 = decryptTest.encrypt(encryptionKey, "piet")

        val m = mapOf(
            "arguments" to listOf(
                listOf(encryptionKey, cipherText1),
                listOf(encryptionKey, null)
            )
        )

        val jsonResponse = handler.handleRequest(m, null)
        with(gson.fromJson(jsonResponse, Success::class.java)) {
            results shouldBe listOf("piet", null)
            numRecords shouldBe 2
        }
    }

    @Test
    fun `test null encryption key`() {
        val handler = Handler()

        val cipherText1 = decryptTest.encrypt(encryptionKey, "piet")

        val m = mapOf(
            "arguments" to listOf(
                listOf(null, cipherText1),
                listOf(null, null)
            )
        )

        val r = handler.handleRequest(m, null)
        with(gson.fromJson(r, Success::class.java)) {
            success shouldBe true
            results shouldBe listOf(null, null)
            numRecords shouldBe 2
        }
    }

    @Test
    fun `test bad encryption key`() {
        val handler = Handler()

        val cipherText1 = decryptTest.encrypt(encryptionKey, "piet")

        val m = mapOf(
            "arguments" to listOf(
                listOf("piet", cipherText1),
                listOf("klaas", null)
            )
        )
        val r = handler.handleRequest(m, null)
        with(gson.fromJson(r, Failure::class.java)) {
            success shouldBe false
        }
    }

    @Test
    fun `test wrong encryptionKey`() {
        val handler = Handler()

        val cipherText1 = "AUr7zSCRjFQiQMxc5WNeN4BUFRnKXtPAXIY="

        val m = mapOf(
            "arguments" to listOf(
                listOf(encryptionKey, cipherText1)
            )
        )

        val r = handler.handleRequest(m, null)
        with(gson.fromJson(r, Failure::class.java)) {
            success shouldBe false
        }
    }
}
