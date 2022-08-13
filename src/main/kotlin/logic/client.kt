package logic

import com.sksamuel.hoplite.ConfigLoader
import infrastructure.Config
import infrastructure.DataBaseConnector
import infrastructure.OauhtParams
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val config = ConfigLoader().loadConfigOrThrow<Config>("/config.yaml")

class Client(users: List<Long>, private val schedules: List<ScheduleConfig>) {

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
        for (user in users) {
            var tweets = listOf<String>()
            transaction {
                tweets = DataBaseConnector.selectAllTweetsFrom(user)
            }
            usersSpitter[user] = TuitterSpitter(tweets)
        }
    }

    fun gibber(user: Long): String {
        val spitter = usersSpitter[user] ?: return ""
        return spitter.spoutGibberish()
    }

    fun postSomething(user: Long) {

        val gibberish = gibber(user)
        val tuitterPoster = TuitterPoster("text", gibberish, oauthConfigs)
        logger.info { "Going to tweet: $gibberish" }
        val postTweet = tuitterPoster.postTweet()
        logger.info { "Result post: $postTweet" }
    }

    fun initScheduler() {
        var initalDelay = 15L
        for (scheduleConfig in this.schedules) {
            logger.info { "Setting scheduler for userId ${scheduleConfig.tweetUserId} " +
                    "running each ${scheduleConfig.intervalInSeconds / 60}m, " +
                    "starting in ${initalDelay / 60} minutes" }
            scheduler.scheduleWithFixedDelay(
                { this.postSomething(scheduleConfig.tweetUserId) },
                initalDelay,
                scheduleConfig.intervalInSeconds,
                TimeUnit.SECONDS)
            initalDelay += 60 * 2 // each task will start tot minutes apart
        }
    }

}