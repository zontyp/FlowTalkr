package engine

import com.fasterxml.jackson.databind.JsonNode

data class NodeResult(
    val outputData: JsonNode,
    val nextNode: String? = null
)
