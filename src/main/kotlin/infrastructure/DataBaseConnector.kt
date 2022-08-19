package infrastructure

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime

object DataBaseConnector {

    fun selectAllTweetsFrom(userId: Long) : List<String> {
        return Tweets.select { Tweets.authorId eq userId }
            .map { it[Tweets.text] }.toList()
    }

    fun insertTweet(tweetIdInput: Long, textInput: String, langInput: String, createdAtInput: LocalDateTime, authorIdInput: Long) {
        Tweets.insert {
            it[tweetId] = tweetIdInput
            it[text] = textInput
            it[lang] = langInput
            it[createdAt] = createdAtInput
            it[authorId] = authorIdInput
        }
    }

}