package io.strmprivacy.aws.lambda.decrypter

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.google.gson.Gson
import com.google.gson.GsonBuilder

private const val LOG_INTERVAL_MS = 10000

/**
 * the AWS λ request handler.
 * The values are already deserialized by the λ runtime (via Jackson).
 *  See here for details of the parameters.
 * https://docs.aws.amazon.com/redshift/latest/dg/udf-creating-a-lambda-sql-udf.html#udf-lambda-json
 */
class Handler : RequestHandler<Map<String, List<List<String?>>>, String> {

    private val gson: Gson = GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create()
    private var logMoment = System.currentTimeMillis()
    private var processedRecords = 0

    /**
     * apply a scalar decrypt to multiple sql rows.
     *
     * Each row contains (encryptionKey, ciphertext).
     * In case of a decryption failure, the call is aborted with an error
     * @param input See link above
     * @return a json representation of Success or Failure
     */
    override fun handleRequest(input: Map<String, List<List<String?>>>, context: Context?): String {
        val arguments = input["arguments"]!!
        val (errorMessage, results) = try {
            null to arguments.map { (encryptionKey, cipherText) ->
                when {
                    encryptionKey == null || cipherText == null -> {
                        null
                    }
                    else -> {
                        Decrypt.decrypt(encryptionKey, cipherText)
                    }
                }
            }
        } catch (e: Throwable) {
            e.message to null
        }
        logOccasionally(arguments, context)
        return gson.toJson(
            when (errorMessage) {
                null -> {
                    Success(results!!)
                }
                else -> {
                    Failure(errorMessage)
                }
            }
        )
    }

    private fun logOccasionally(
        arguments: List<List<String?>>,
        context: Context?
    ) {
        processedRecords += arguments.size
        val now = System.currentTimeMillis()
        if (now - logMoment > LOG_INTERVAL_MS) {
            context?.logger?.log("Processed $processedRecords records")
            logMoment = now
            processedRecords = 0
        }
    }
}
