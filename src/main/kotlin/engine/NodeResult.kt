package engine

import com.fasterxml.jackson.databind.JsonNode

data class NodeResult(
    val outputData: JsonNode,
    val routeKey: String? = null,
    val stateUpdates: Map<String, JsonNode> = emptyMap()
)
