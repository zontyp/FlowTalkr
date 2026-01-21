package workflow

import engine.Node

data class Workflow(
    val id:String,
    val nodesNameMap: Map<String, Node>,
    val triggers: List<TriggerDefinition> = emptyList()
)
