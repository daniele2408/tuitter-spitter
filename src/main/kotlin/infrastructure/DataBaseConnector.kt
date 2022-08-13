package infrastructure

import org.jetbrains.exposed.sql.select

object DataBaseConnector {

    fun selectAllTweetsFrom(userId: Long) : List<String> {
        return Tweets.select { Tweets.authorId eq userId }
            .map { it[Tweets.text] }.toList()
    }

}