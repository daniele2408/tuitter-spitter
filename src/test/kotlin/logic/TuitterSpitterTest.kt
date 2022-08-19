package logic

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

internal class TuitterSpitterTest {

    @Test
    fun spitter() {

        val tweets = listOf(
            "Qualcuno può per favore concedere attenzioni a questo signore il cui piano terapeutico non mi sembra ben bilanciato? Mettetegli dei cuoricini, orsù, sennò me lo trovo sotto casa col Giovane Holden. https://t.co/sfQHVZsCAK",
            "@FedeGaudi @ErranteItaliano @giorgiogilestro @nunziapenelope @cribart @LaStampa Sono sempre ammiratissima da chi ha la pazienza di rispondere nel merito a gente che tanto il merito non ha i mezzi per volerlo capire e fa di tutto per renderlo evidente.",
            "@ultrajanko @gloquenzi Chissà cosa pensa voglia dire «inedia» (ma pure «influencer»)"
        )

        val tuitterSpitter = TuitterSpitter(tweets)

        for (i in 1..10) {
            val gibberish = tuitterSpitter.buildSentence()
            assertNotNull(gibberish)
        }


    }

    @Test
    fun capitalize() {
        val capitalizeSentence = "sono una sentenza. molto brutta. ma molto brutta".prettifySentence()

        kotlin.test.assertEquals("Sono una sentenza. Molto brutta. Ma molto brutta.", capitalizeSentence)
    }

}