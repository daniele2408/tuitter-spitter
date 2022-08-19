package logic

import infrastructure.DataBaseConnector
import infrastructure.DataBaseSingleton
import infrastructure.Tweets
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ClientTest {

    @BeforeTest
    fun initDb() {

        DataBaseSingleton.dbMem

    }

    @Test
    fun postSomething() {

        transaction {

            SchemaUtils.create(Tweets)

            DataBaseConnector.insertTweet(
                123L,
                "Sono un tweet",
                "it",
                LocalDateTime.of(2022, 1, 1, 13, 14),
                456L
            )
            DataBaseConnector.insertTweet(
                124L,
                "Sono un altro tweet",
                "it",
                LocalDateTime.of(2022, 1, 1, 13, 14),
                456L
            )

            val client = Client(listOf(456L to 60L))

            val gibberFromUser = client.gibberFromUser(456L)

            assertNotNull(gibberFromUser)

            SchemaUtils.drop(Tweets)
        }

    }

    @Test
    fun postSomethingButNoAuthor() {

        transaction {

            SchemaUtils.create(Tweets)

            DataBaseConnector.insertTweet(
                123L,
                "Sono un tweet",
                "it",
                LocalDateTime.of(2022, 1, 1, 13, 14),
                456L
            )
            DataBaseConnector.insertTweet(
                124L,
                "Sono un altro tweet",
                "it",
                LocalDateTime.of(2022, 1, 1, 13, 14),
                456L
            )

            val client = Client(listOf(456L to 60L))

            val gibberFromUser = client.gibberFromUser(789L)

            assertTrue(gibberFromUser.isEmpty())

            SchemaUtils.drop(Tweets)
        }

    }

    @Test
    fun postSomethingButNoTweets() {

        transaction {

            SchemaUtils.create(Tweets)

            val client = Client(listOf(456L to 60L))

            val gibberFromUser = client.gibberFromUser(789L)

            assertTrue(gibberFromUser.isEmpty())

            SchemaUtils.drop(Tweets)
        }

    }

    @Test
    fun refreshCollectTweet() {

        transaction {

            SchemaUtils.create(Tweets)

            DataBaseConnector.insertTweet(
                123L,
                "Sono un tweet",
                "it",
                LocalDateTime.of(2022, 1, 1, 13, 14),
                456L
            )

            val client = Client(listOf(123L to 60L, 789L to 60L))

            val gibberFromUserEmpty = client.gibberFromUser(789L)
            assertTrue(gibberFromUserEmpty.isEmpty())

            DataBaseConnector.insertTweet(
                124L,
                "Sono un altro tweet",
                "it",
                LocalDateTime.of(2022, 1, 1, 13, 14),
                789L
            )

            client.collectTweets()

            val gibberFromUserNonEmpty = client.gibberFromUser(789L)
            assertFalse(gibberFromUserNonEmpty.isEmpty())

            SchemaUtils.drop(Tweets)
        }

    }


}