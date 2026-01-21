package workflow


sealed class TriggerDefinition {

    data class Cron(
        val id: String,
        val expression: String,
        val startNode: String
    ) : TriggerDefinition()

    data class Telegram(
        val id: String,
        val startNode: String
    ) : TriggerDefinition()
}

