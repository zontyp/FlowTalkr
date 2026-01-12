package workflow

import com.fasterxml.jackson.databind.JsonNode
/*
Layer	Purpose
NodeDefinition	What the workflow says, json parsed to object
WorkflowNode	What the engine executes
Node	How execution happens
 */
data class NodeDefinition(
    val name: String,
    val type: String,
    val config: JsonNode,
    val next: Map<String, String> = emptyMap()
)
