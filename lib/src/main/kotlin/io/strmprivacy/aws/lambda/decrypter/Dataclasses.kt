package io.strmprivacy.aws.lambda.decrypter

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * success response lambda for Redshift udf
 */
data class Success(
    @Expose
    val results: List<String?>,
    @Expose
    @SerializedName("num_records")
    val numRecords: Int = results.size,
    @Expose
    val success: Boolean = true
)
data class Failure(
    /**
     * failure response lambda for Redshift udf
     */
    @Expose
    @SerializedName("error_msg")
    val errorMsg: String,
    @Expose
    val success: Boolean = false
)
