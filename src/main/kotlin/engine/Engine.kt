package engine

import com.fasterxml.jackson.databind.JsonNode
import workflow.Workflow

class Engine {

    fun run(
        workflow: Workflow,
        initialInput: JsonNode
    ): JsonNode {

        var currentNodeName: String? = workflow.start
        var currentData: JsonNode = initialInput
        val state = mutableMapOf<String, JsonNode>()

        while (currentNodeName != null) {
            val wfNode = workflow.nodes[currentNodeName]
                ?: error("Unknown node: $currentNodeName")

            val result = wfNode.node.execute(
                inputData = currentData,
                state = state
            )

            currentData = result.outputData

            // 🔑 routing decision
            val routeKey = when {
                result.outputData.has("condition") ->
                    if (result.outputData["condition"].asBoolean()) "true" else "false"

                else -> "default"
            }

            currentNodeName = wfNode.next[routeKey]
        }

        return currentData
    }
}
