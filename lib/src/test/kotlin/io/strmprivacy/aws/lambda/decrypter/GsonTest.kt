package io.strmprivacy.aws.lambda.decrypter

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GsonTest {
    private val gson: Gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    @Test
    fun `test list of list`() {
        val json = """[ 
            |["a", "b"]
            |,[null, "c"]
            |,["d", null]
            |,[null,null]
            |]
        """.trimMargin()
        val typeToken = object : TypeToken<List<List<String?>>>() {}.type

        val parsed = gson.fromJson<List<List<String?>>>(json, typeToken)
        parsed.size shouldBe 4
        parsed[0][0] shouldBe "a"
        parsed[3][1] shouldBe null
    }
}
