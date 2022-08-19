package logic

import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.random.Random

private const val START_SIGN: String = "$$$"
private const val END_SIGN: String = "£££"
private const val MAX_TWEET_LENGTH: Int = 280

private val PUNCTUATION_SYMBOLS: Pattern = Pattern.compile("[()»«;?*\":\\n…]")
private val MULTIPLE_SPACE: Pattern = Pattern.compile("/  +/g")
private val SPACED_COMMA: Pattern = Pattern.compile(" ,")

fun String.wrapSentenceStartEndSigns(): String = run { "$START_SIGN $this $END_SIGN" }
fun String.replacePunctuation(): String = run { PUNCTUATION_SYMBOLS.matcher(this).replaceAll(" ") }
fun String.removeMultipleSpaces(): String = run { MULTIPLE_SPACE.matcher(this).replaceAll(" ") }
fun String.removeSpaceBeforeComma(): String = run { SPACED_COMMA.matcher(this).replaceAll(",") }
fun String.isTwitterTag(): Boolean = run { """@[A-Za-z0-9_]+""".toRegex().matches(this) }
fun String.isHttpsUrl(): Boolean = run { """https.*""".toRegex().matches(this) }
fun String.isShortenedLink(): Boolean = run { """.*t.co.*""".toRegex().matches(this) }
fun String.removePattern(pattern: String): String = run { this.replace(Regex(pattern), "") }
fun String.prettifySentence() : String {
    var res = this
    val pattern = Regex("(?<=\\. )[a-z]")
    val find = pattern.findAll(this)
    for (match in find) {
        val group: String = match.value
        res = res.replace(pattern, group.uppercase())
    }
    res = res.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    if (res.last() != '.' && res.last() != '!') return "$res."
    return res
}

class FrequencyWordsSentenceBuilder(words: List<String>) {
    private val dictWords: MutableMap<String, MutableMap<String, Int>> = mutableMapOf<String, MutableMap<String, Int>>()
        .withDefault { mutableMapOf<String, Int>().withDefault { 0 } }
    var distroLengths: Map<Int, Float> = mapOf()

    init {

        val pairs = words.zipWithNext()
            .toList()
        // Transform tweets
        val toSortedMap = words.groupingBy { it.length }.eachCount()
            .entries.associate { it.key to it.value }.toSortedMap()
        distroLengths = toSortedMap.entries
            .associate {
                it.key to toSortedMap.entries.takeWhile { entry -> entry.key <= it.key }
                    .sumOf { entry -> entry.value }.toFloat() / words.size
            }
            .toMap()

        addDoubleEntry(dictWords, START_SIGN, pairs[0].first)
        addDoubleEntry(dictWords, pairs[pairs.size-1].second, END_SIGN)

        for ((prev, next) in pairs) {
            dictWords.getValue(prev).merge(next, 1, Int::plus)
            addDoubleEntry(dictWords, prev, next)
        }
    }

    private fun addDoubleEntry(dictWords: MutableMap<String, MutableMap<String, Int>>, prev: String, next: String) {
        dictWords.putIfAbsent(prev, mutableMapOf<String, Int>().withDefault { 0 })
        dictWords[prev]?.putIfAbsent(next, 0)
        dictWords[prev]?.merge(next, 1, Int::plus)
    }

    private fun getNextWord(prev: String) : String {
        val mutableMap = dictWords[prev] ?: return END_SIGN
        return mutableMap.entries.map { entry -> List(entry.value) { entry.key } }.flatten().random()
    }

    fun buildSentence() : String {
        var gibberish = ""

        var someWord = getNextWord(START_SIGN)

        while (someWord != END_SIGN) {
            gibberish += "$someWord "
            someWord = getNextWord(someWord)
            if (
                gibberish.split(" ").size > 5 &&
                dictWords[someWord]?.keys?.contains(END_SIGN) == true &&
                gibberish.last() == '.' &&
                shouldIStop(gibberish.length)
            ) someWord = END_SIGN
        }

        val indexOfLastSpace = gibberish.indexOfLast { it == ' ' }
        gibberish = if (indexOfLastSpace > MAX_TWEET_LENGTH) {
            gibberish.slice(0 until gibberish.slice(0 until MAX_TWEET_LENGTH).indexOfLast { it == ' ' })
        } else {
            gibberish.slice(0 until indexOfLastSpace)
        }

        return gibberish.prettifySentence()
    }

    private fun shouldIStop(actualLength: Int) : Boolean {
        if (actualLength >= MAX_TWEET_LENGTH) return true

        val minOf = distroLengths.keys.minBy { abs(it - actualLength) }
        val prob = distroLengths[minOf] ?: return false
        return Random.nextFloat() < prob
    }

}

class TuitterSpitter(tweets: List<String>) {

    private val frequencyWordsSentenceBuilder = FrequencyWordsSentenceBuilder(sanitizeTweetsAndChain(tweets))

    private fun sanitizeTweetsAndChain(tweets: List<String>): List<String> {
        return tweets
            .map { it.wrapSentenceStartEndSigns() }
            .map { it.replacePunctuation() }
            .map { it.removeMultipleSpaces() }
            .map { it.removeSpaceBeforeComma() }
            .map { it.lowercase() }
            .flatMap { it.split(" ") }
            .asSequence()
            .filter { it != "rt" }
            .filter { !it.isTwitterTag() }
            .filter { !it.isHttpsUrl() }
            .filter { !it.isShortenedLink() }
            .filter { it.isNotEmpty() }
            .map {  it.removePattern("""@""") }
            .toList()
    }

    fun buildSentence(): String {
        return frequencyWordsSentenceBuilder.buildSentence()
    }

}