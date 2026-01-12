package engine

import com.fasterxml.jackson.databind.JsonNode

data class NodeResult(
    val outputData: JsonNode,
    val stateUpdates: Map<String, JsonNode> = emptyMap()
)
