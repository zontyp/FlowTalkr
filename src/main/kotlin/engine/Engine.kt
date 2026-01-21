package engine

import com.fasterxml.jackson.databind.JsonNode
import workflow.Workflow

class Engine {

    fun run(
        workflow: Workflow,
        startNode: String,
        initialInput: JsonNode
    ): JsonNode {

        var currentNodeName: String? = startNode
        var currentData: JsonNode = initialInput
        val state = mutableMapOf<String, JsonNode>()

        while (currentNodeName != null) {

            val currentNode = workflow.nodesNameMap[currentNodeName]
                ?: error("Unknown node: $currentNodeName")

            println("▶ Executing node: $currentNodeName")

            val result = currentNode.execute(
                inputData = currentData,
                state = state
            )

            currentData = result.outputData
            currentNodeName = result.nextNode
            println("✔ Output Data : $currentData")
            println("✔ Next node: $currentNodeName")
            println("----")
        }

        return currentData
    }
}
