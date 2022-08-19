package logic

import com.sksamuel.hoplite.ConfigLoader
import infrastructure.Config
import infrastructure.DataBaseConnector
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val config = ConfigLoader().loadConfigOrThrow<Config>("/config.yaml")

class Client(usersFreqPairs: List<Pair<Long, Long>>) {

    private val users: List<Long> = usersFreqPairs.map { it.first }
    private val schedules: List<ScheduleConfig> = usersFreqPairs.map { ScheduleConfig(it.first, it.second) }
    private val logger = KotlinLogging.logger {}
    private val scheduler = Executors.newScheduledThreadPool(schedules.size)
    private val oauthConfigs = OauthConfigs(
        "https://api.twitter.com/2/tweets",
        config.oauthParams.consumerKey,
        config.oauthParams.consumerSecret,
        "HMAC-SHA1",
        config.oauthParams.token,
        config.oauthParams.tokenSecret,
        "1.0"
    )

    var usersSpitter: MutableMap<Long, TuitterSpitter> = mutableMapOf()

    init {
        collectTweets()
    }

    internal fun gibberFromUser(user: Long): String {
        val tuitterSpitter = usersSpitter[user]
        if (tuitterSpitter == null) {
            logger.warn { "There's not spitter for user $user, return empty string" }
            return ""
        }
        return tuitterSpitter.buildSentence()
    }

    fun postSomethingFromUser(user: Long) {

        try {
            val gibberish = gibberFromUser(user)
            val tuitterPoster = TuitterPoster("text", gibberish, oauthConfigs)
            logger.info { "Going to tweet: $gibberish" }
            val postTweet = tuitterPoster.postTweet() ?: return
            logger.info { "Result post: $postTweet" }
        } catch (e: Exception) {
            logger.error { "Error while calling postSomethingFromUser: $e" }
        }
    }

    fun initSchedulers() {
        var initialDelay = 15L
        for (scheduleConfig in this.schedules) {
            logger.info { "Setting scheduler for userId ${scheduleConfig.tweetUserId} " +
                    "running each ${scheduleConfig.intervalInSeconds / 60}m, " +
                    "starting in ${initialDelay / 60} minutes" }
            scheduler.scheduleWithFixedDelay(
                { this.postSomethingFromUser(scheduleConfig.tweetUserId) },
                initialDelay,
                scheduleConfig.intervalInSeconds,
                TimeUnit.SECONDS)
            initialDelay += 60 * 37 // each task will start tot minutes apart
        }
        // fetch new tweets from db
        scheduler.scheduleWithFixedDelay(
            { this.collectTweets() },
            1L,
            1,
            TimeUnit.DAYS
        )
    }

    internal fun collectTweets() {
        for (user in users) {
            var tweets = listOf<String>()
            transaction {
                tweets = DataBaseConnector.selectAllTweetsFrom(user)
            }
            if (tweets.isNotEmpty()) usersSpitter[user] = TuitterSpitter(tweets)
            else logger.warn { "No tweets found for user $user" }
        }
    }

}