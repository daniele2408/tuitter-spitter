package logic

import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.random.Random

class TuitterSpitter(tweets: List<String>) {

    private val MAX_TWEET_LENGTH: Int = 280
    private val PUNCT_SYMBOLS: Pattern = Pattern.compile("[()»«;?*\":\\n…]")
    private val MULTIPLE_SPACE: Pattern = Pattern.compile("/  +/g")
    private val SPACED_COMMA: Pattern = Pattern.compile(" ,")
    val START_SIGN: String = "$$$"
    val END_SIGN: String = "£££"
    val dictWords: MutableMap<String, MutableMap<String, Int>> = mutableMapOf<String, MutableMap<String, Int>>()
        .withDefault { mutableMapOf<String, Int>().withDefault { 0 } }
    var distroLengths: Map<Int, Float> = mapOf()

    init {

        val pairs: List<Pair<String, String>> = tweets
            .map { "$START_SIGN $it $END_SIGN" }
            .map { PUNCT_SYMBOLS.matcher(it).replaceAll(" ") }
            .map { MULTIPLE_SPACE.matcher(it).replaceAll(" ") }
            .map { SPACED_COMMA.matcher(it).replaceAll(",") }
            .map { it.lowercase() }
            .flatMap { it.split(" ") }
            .asSequence()
            .filter { it != "rt" }
            .filter { !"""@[A-Za-z0-9_]+""".toRegex().matches(it) }
            .filter { !"""https.*""".toRegex().matches(it) }
            .filter { !""".*t.co.*""".toRegex().matches(it) }
            .filter { it != "" }
            .map {  it.replace(Regex("""@"""), "") }
            .zipWithNext()
            .toList()

        val toSortedMap = tweets.groupingBy { it.length }.eachCount()
            .entries.associate { it.key to it.value }.toSortedMap()
        distroLengths = toSortedMap.entries
            .associate {
                it.key to toSortedMap.entries.takeWhile { entry -> entry.key <= it.key }.map { it.value }
                    .sum().toFloat() / tweets.size
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

    fun getNextWord(prev: String) : String {
        val mutableMap = dictWords[prev] ?: return END_SIGN
        return mutableMap.entries.map { entry -> List(entry.value) { entry.key } }.flatten().random()
    }

    fun spoutGibberish() : String {
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

        return capitalizeSentence(gibberish)
    }

    fun shouldIStop(actualLength: Int) : Boolean {
        if (actualLength >= MAX_TWEET_LENGTH) return true

        val minOf = distroLengths.keys.minBy { abs(it - actualLength) }
        val prob = distroLengths[minOf] ?: return false
        return Random.nextFloat() < prob
    }

    fun capitalizeSentence(sentence: String) : String {
        var res = sentence
        val pattern = Regex("(?<=\\. )[a-z]")
        val find = pattern.findAll(sentence)
        for (match in find) {
            val group: String = match.value
            res = res.replace(pattern, group.uppercase())
        }
        res = res.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        if (res.last() != '.' && res.last() != '!') return "$res."
        return res
    }

}