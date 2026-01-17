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

            println("▶ Executing node: $currentNodeName")

            val result = wfNode.node.execute(
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
