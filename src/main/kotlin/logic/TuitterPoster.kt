package logic

import mu.KotlinLogging
import org.json.JSONObject
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

val percentEncoderMap = mapOf(
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

private fun String.percentEncode(): String = run {
    var res = this
    res = res.replace("""%""", "%25")
    percentEncoderMap.forEach {(k,v) -> res = res.replace(k, v)}
    return res
}

private fun Pair<String, String>.percentEncode(): Pair<String, String> = run {
    Pair(this.first.percentEncode(), this.second.percentEncode())
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
    val signingKey = "${consumerSecret.percentEncode()}&${tokenSecret.percentEncode()}"
}

class TuitterPoster(
    private val keyParam: String,
    private val text: String,
    configs: OauthConfigs,
    private val urlParams: Map<String, String>? = mapOf()
) {

    private val logger = KotlinLogging.logger {}
    private val encoder: Base64.Encoder = Base64.getEncoder()

    private val baseUrl = configs.baseUrl
    private val consumerKey = configs.consumerKeyParam
    private val signatureMethod = configs.signatureMethodParam
    private val token = configs.tokenParam
    private val version = configs.oauthVersionParam

    private val signingKey = configs.signingKey


    private fun percentEncode(data: Pair<String, String>) : Pair<String, String> {
        return Pair(data.first.percentEncode(), data.second.percentEncode())
    }

    internal fun createSignature(timestampParam: Pair<String, String>, onceParam: Pair<String, String>): Pair<String, String> {
        val sha1Hmac = Mac.getInstance("HmacSHA1")
        val secretKey = SecretKeySpec(signingKey.toByteArray(Charsets.UTF_8), "HmacSHA1")
        sha1Hmac.init(secretKey)

        val signatureBaseString = createSignatureBaseString(timestampParam, onceParam)
        val doFinal: ByteArray = sha1Hmac.doFinal(signatureBaseString.toByteArray(Charsets.UTF_8))
        val encodeToString: String =
            Base64.getEncoder().encodeToString(doFinal)
        // For base64
         return "oauth_signature" to encodeToString
    }

    internal fun createSignatureBaseString(timestampParam: Pair<String, String>, onceParam: Pair<String, String>) : String {
        val encodedBaseUrl = baseUrl.percentEncode()
        val parameterString = createParameterString(timestampParam, onceParam).percentEncode()
        return "POST&$encodedBaseUrl&$parameterString"
    }

    private fun createParameterString(timestampStr: Pair<String, String>, onceParam: Pair<String, String>): String {
        val listA = listOf(
            consumerKey,
            onceParam,
            signatureMethod,
            timestampStr,
            token,
            version
        ).map { it.percentEncode() }
        val listB = urlParams?.entries?.map { it.key to it.value } ?.toList() ?: listOf()
        return (listA + listB).sortedBy { it.first }.joinToString("&") { "${it.first}=${it.second}" }
    }

    internal fun generateOnceParam() : Pair<String, String> {
        val bytes = ByteArray(32)
        val nextBytes = Random.Default.nextBytes(bytes)
        val encode = encoder.encode(nextBytes).toString()
        return "oauth_nonce" to Regex("[^A-Za-z0-9 ]").replace(encode, "")
    }

    internal fun generateTimestamp() : Pair<String, String> {
        return "oauth_timestamp" to (System.currentTimeMillis() / 1000).toString()
    }

    internal fun createParameterOauth(): String {
        val onceParam = generateOnceParam()
        val timestampParam = generateTimestamp()
        return "OAuth " + listOf(
            percentEncode(consumerKey),
            percentEncode(onceParam),
            percentEncode(createSignature(timestampParam, onceParam)),
            percentEncode(signatureMethod),
            percentEncode(timestampParam),
            percentEncode(token),
            percentEncode(version)
        ).sortedBy { it.first }.joinToString(",") { "${it.first}=\"${it.second}\"" }
    }

    fun postTweet() : JSONObject? {
        val headers = mapOf(
            "Authorization" to createParameterOauth(),
            "Content-Type" to "application/json"
        )
        val postResult = khttp.post(
            url = baseUrl,
            headers = headers,
            json = mapOf(keyParam to text)
        )
        if (postResult.statusCode == 201) return postResult.jsonObject
        logger.error { "Error POST result, status code is ${postResult.statusCode}: response is ${postResult.jsonObject}" }
        return null
    }


}