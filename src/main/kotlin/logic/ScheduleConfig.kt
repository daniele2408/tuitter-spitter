package logic

data class ScheduleConfig(
    val tweetUserId: Long,
    val intervalInSeconds: Long = 60 * 60 * 10,
)