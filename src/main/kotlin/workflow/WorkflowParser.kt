package workflow

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
//read json and create list of nodedefs
class WorkflowParser(
    private val mapper: ObjectMapper
) {

    fun parse(json: JsonNode): WorkflowDefinition {
        val start = json["start"]
            ?: error("Missing 'start' in workflow")

        val nodesJson = json["nodes"]
            ?: error("Missing 'nodes' in workflow")

        val nodes = nodesJson.map { nodeJson ->
            NodeDefinition(
                name = nodeJson["name"]?.asText()
                    ?: error("Node missing 'name'"),
                type = nodeJson["type"]?.asText()
                    ?: error("Node missing 'type'"),
                config = nodeJson["config"]
                    ?: mapper.createObjectNode(),
                next = parseNext(nodeJson["next"])
            )
        }


        return WorkflowDefinition(
            start = start.asText(),
            nodes = nodes
        )
    }

    private fun parseNext(nextNode: JsonNode?): Map<String, String> {
        if (nextNode == null || nextNode.isNull) {
            return emptyMap()
        }

        if (!nextNode.isObject) {
            error("'next' must be an object")
        }

        return nextNode.fields().asSequence().associate { (key, value) ->
            key to value.asText()
        }
    }
}
