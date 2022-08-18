package logic

import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Test

internal class TuitterPosterTest {

    @Test
    fun createSignature() {
        val oauthConfigs = OauthConfigs(
            "https://api.twitter.com/1.1/statuses/update.json",
            "xvz1evFS4wEEPTGEFPHBog",
            "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw",
            "HMAC-SHA1",
            "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb",
            "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE",
            "1.0"
        )

//        val tuitterPoster = TuitterPoster("status", "Hello Ladies+Gentlemen, a signed OAuth request!", oauthConfigs)

        val spyPoster = spyk(TuitterPoster(
            "status",
            "Hello Ladies + Gentlemen, a signed OAuth request!",
            oauthConfigs,
            mapOf("include_entities" to "true")
        ))

        val timestampParam = Pair("oauth_timestamp", "1318622958")
        every { spyPoster.generateTimestamp() } returns timestampParam
        val onceParam = Pair("oauth_nonce", "kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg")
        every { spyPoster.generateOnceParam() } returns onceParam

        val signatureBaseString = spyPoster.createSignatureBaseString(timestampParam, onceParam)

        val expectedSignatureBaseString = """
            POST&https%3A%2F%2Fapi.twitter.com%2F1.1%2Fstatuses%2Fupdate.json&include_entities%3Dtrue%26oauth_consumer_key%3Dxvz1evFS4wEEPTGEFPHBog%26oauth_nonce%3DkYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1318622958%26oauth_token%3D370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb%26oauth_version%3D1.0
        """.trimIndent()

        kotlin.test.assertEquals(expectedSignatureBaseString, signatureBaseString)

        val oauthSignature = spyPoster.createSignature(timestampParam, onceParam)
        val expectedSignature = "swB2/K4QtoSNF7fQfLzyNivuoj4="

        kotlin.test.assertEquals(expectedSignature, oauthSignature.second)

        val createParameterOauth = spyPoster.createParameterOauth()
        val expected = """
            OAuth oauth_consumer_key="xvz1evFS4wEEPTGEFPHBog",oauth_nonce="kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg",oauth_signature="swB2%2FK4QtoSNF7fQfLzyNivuoj4%3D",oauth_signature_method="HMAC-SHA1",oauth_timestamp="1318622958",oauth_token="370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb",oauth_version="1.0"
        """.trimIndent()

        kotlin.test.assertEquals(expected, createParameterOauth)

    }

}