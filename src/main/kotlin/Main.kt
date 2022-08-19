import com.sksamuel.hoplite.ConfigLoader
import infrastructure.Config
import infrastructure.DataBaseSingleton
import logic.Client

val config = ConfigLoader().loadConfigOrThrow<Config>("/config.yaml")

fun main(args: Array<String>) {
    DataBaseSingleton.db

    val usersList = config.twitterUsers.users
    val client = Client(
        usersList.map { it to 60L * 77L }
    )

    usersList.forEach { user -> client.postSomethingFromUser(user) }
    client.initSchedulers()


}