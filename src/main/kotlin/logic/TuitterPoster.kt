package logic

import org.json.JSONObject
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

val percentEncoderMap = mapOf<String, String>(
    """ """ to "%20",
    """!""" to "%21",
    """#""" to "%23",
    """$""" to "%24",
    """&""" to "%26",
    """'""" to "%27",
    """(""" to "%28",
    """)""" to "%29",
    """*""" to "%2A",
    """+""" to "%2B",
    """,""" to "%2C",
    """/""" to "%2F",
    """:""" to "%3A",
    """;""" to "%3B",
    """=""" to "%3D",
    """?""" to "%3F",
    """@""" to "%40",
    """[""" to "%5B",
    """]""" to "%5D"
)

private fun percentEncode(data: String) : String {
    var res = data
    res = res.replace("""%""", "%25")
    percentEncoderMap.forEach {(k,v) -> res = res.replace(k, v)}
    return res
}

data class OauthConfigs(
    val baseUrl: String,
    val consumerKey: String,
    val consumerSecret: String,
    val signatureMethod: String,
    val token: String,
    val tokenSecret: String,
    val oauthVersion: String
) {
    val consumerKeyParam = "oauth_consumer_key" to consumerKey
    val signatureMethodParam = "oauth_signature_method" to signatureMethod
    val tokenParam = "oauth_token" to token
    val oauthVersionParam = "oauth_version" to oauthVersion
    val signingKey = "${percentEncode(consumerSecret)}&${percentEncode(tokenSecret)}"
}

class TuitterPoster(val keyParam: String, val text: String, configs: OauthConfigs, val urlParams: Map<String, String>? = mapOf()) {

    val body: BodySingleValue = BodySingleValue(keyParam, text)
    val encoder: Base64.Encoder = Base64.getEncoder()

    val BASE_URL = configs.baseUrl
    val CONSUMER_KEY = configs.consumerKeyParam
    val SIGNATURE_METHOD = configs.signatureMethodParam
    val TOKEN = configs.tokenParam
    val VERSION = configs.oauthVersionParam

    val SIGNING_KEY = configs.signingKey


    private fun percentEncode(data: Pair<String, String>) : Pair<String, String> {
        return Pair(percentEncode(data.first), percentEncode(data.second))
    }

    inner class BodySingleValue(val key: String, val value: String) {

        val param = key to value
        val payload = mapOf<String, String>(param)

        override fun toString(): String {
            return "${key}=${percentEncode(value)}"
        }

    }

    fun createSignature(timestampParam: Pair<String, String>, onceParam: Pair<String, String>): Pair<String, String> {
        val sha1Hmac = Mac.getInstance("HmacSHA1")
        val secretKey = SecretKeySpec(SIGNING_KEY.toByteArray(Charsets.UTF_8), "HmacSHA1")
        sha1Hmac.init(secretKey)

        val signatureBaseString = createSignatureBaseString(timestampParam, onceParam)
        val doFinal: ByteArray = sha1Hmac.doFinal(signatureBaseString.toByteArray(Charsets.UTF_8))
        val encodeToString: String =
            Base64.getEncoder().encodeToString(doFinal)
        // For base64
         return "oauth_signature" to encodeToString
    }

    fun createSignatureBaseString(timestampParam: Pair<String, String>, onceParam: Pair<String, String>) : String {
        val encodedBaseUrl = percentEncode(BASE_URL)
        val parameterString = percentEncode(createParameterString(timestampParam, onceParam))
        return "POST&$encodedBaseUrl&$parameterString"
    }

    fun createParameterString(timestampStr: Pair<String, String>, onceParam: Pair<String, String>): String {
        val listA = listOf(
//            percentEncode(body.param),  // AAAAAAHH
            percentEncode(CONSUMER_KEY),
            percentEncode(onceParam),
            percentEncode(SIGNATURE_METHOD),
            percentEncode(timestampStr),
            percentEncode(TOKEN),
            percentEncode(VERSION)
        )
        val listB = urlParams?.entries?.map { it.key to it.value } ?.toList() ?: listOf()
        return (listA + listB).sortedBy { it.first }.joinToString("&") { "${it.first}=${it.second}" }
    }

    fun generateOnceParam() : Pair<String, String> {
        val bytes = ByteArray(32)
        val nextBytes = Random.Default.nextBytes(bytes)
        val encode = encoder.encode(nextBytes).toString()
        return "oauth_nonce" to Regex("[^A-Za-z0-9 ]").replace(encode, "")
    }

    fun generateTimestamp() : Pair<String, String> {
        return "oauth_timestamp" to (System.currentTimeMillis() / 1000).toString()
    }

    fun createParameterOauth(): String {
        val onceParam = generateOnceParam()
        val timestampParam = generateTimestamp()
        return "OAuth " + listOf(
            percentEncode(CONSUMER_KEY),
            percentEncode(onceParam),
            percentEncode(createSignature(timestampParam, onceParam)),
            percentEncode(SIGNATURE_METHOD),
            percentEncode(timestampParam),
            percentEncode(TOKEN),
            percentEncode(VERSION)
        ).sortedBy { it.first }.joinToString(",") { "${it.first}=\"${it.second}\"" }
    }

    fun postTweet() : JSONObject {
        val headers = mapOf(
            "Authorization" to createParameterOauth(),
            "Content-Type" to "application/json"
        )
        val postResult = khttp.post(
            url = BASE_URL,
            headers = headers,
            json = mapOf(keyParam to text)
        )
        return postResult.jsonObject
    }


}