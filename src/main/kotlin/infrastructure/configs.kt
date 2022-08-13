package infrastructure

data class OauhtParams(
    val consumerKey: String,
    val consumerSecret: String,
    val token: String,
    val tokenSecret: String,
)

data class TwitterUsers(
    val users: List<Long>
)

data class Database(
    val uri: String
)

data class Config(
    val twitterUsers: TwitterUsers,
    val oauthParams: OauhtParams,
    val database: Database
)
