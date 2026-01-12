package engine

import com.fasterxml.jackson.databind.JsonNode

interface Node {

    val name: String
    val type: String
    val stateAccess: StateAccess
    val configData: JsonNode

    fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult
}
