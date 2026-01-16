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
            println("executing")
            println (wfNode.node)
            println(wfNode.next)
            println("executing ends")
            val result = wfNode.node.execute(
                inputData = currentData,
                state = state
            )

            currentData = result.outputData
            println("output data is ")
            println(currentData)
            println("output data ends ")

            // 🔑 routing decision
            var routeKey = result.routeKey ?: "default"
            if(routeKey !in wfNode.next)
                routeKey = "default"
            currentNodeName = wfNode.next[routeKey]
            println("printing route key ")
            println(routeKey)
            println(currentNodeName)
            println("printing route key ends ")
        }

        return currentData
    }
}
