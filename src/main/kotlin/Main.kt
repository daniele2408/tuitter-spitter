import com.sksamuel.hoplite.ConfigLoader
import infrastructure.Config
import infrastructure.DataBaseSingleton
import infrastructure.TwitterUsers
import logic.Client
import logic.ScheduleConfig

val config = ConfigLoader().loadConfigOrThrow<Config>("/config.yaml")

fun main(args: Array<String>) {
    DataBaseSingleton.db

    val usersList = config.twitterUsers.users
    val client = Client(
        usersList,
        usersList.map { ScheduleConfig(it, 60L * 77L) }
    )

    usersList.forEach { user -> client.postSomething(user) }
    client.initScheduler()


}