package infrastructure

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Tweets : IntIdTable() {
    val tweetId: Column<Long> = long("tweetId").uniqueIndex()
    val text: Column<String> = varchar("text", 500)
    val lang: Column<String> = varchar("language", 50)
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val authorId: Column<Long> = long("author_id")
}
