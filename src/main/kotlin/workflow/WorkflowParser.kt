package workflow

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class WorkflowParser(
    private val mapper: ObjectMapper
) {

    fun parse(json: JsonNode): WorkflowDefinition {

        val id = json["id"]?.asText()
            ?: error("Missing 'id' in workflow")

        val nodesJson = json["nodes"]
            ?: error("Missing 'nodes' in workflow")

        val nodes = nodesJson.map { nodeJson ->
            NodeDefinition(
                name = nodeJson["name"]?.asText()
                    ?: error("Node missing 'name'"),
                type = nodeJson["type"]?.asText()
                    ?: error("Node missing 'type'"),
                config = nodeJson["config"]
                    ?: mapper.createObjectNode()
            )
        }

        println(json["triggers"])
        val triggers = json["triggers"]?.map { triggerJson ->
            when (triggerJson["type"]?.asText()) {
                "cron" -> TriggerDefinition.Cron(
                    id = triggerJson["id"]?.asText()
                        ?: error("Cron trigger missing 'id'"),
                    startNode = triggerJson["startNode"]?.asText()
                        ?: error("Cron trigger missing 'expression'"),
                    expression = triggerJson["expression"]?.asText()
                        ?: error("Cron trigger missing 'expression'")
                )
            "telegram" -> TriggerDefinition.Telegram(
                    id = triggerJson["id"]?.asText()
                        ?: error("Cron trigger missing 'id'"),
                    startNode = triggerJson["startNode"]?.asText()
                        ?: error("Cron trigger missing 'expression'")

                )
                else -> error("Unknown trigger type")
            }
        } ?: emptyList()

        return WorkflowDefinition(
            id = id,

            nodes = nodes,
            triggers = triggers
        )
    }
}
