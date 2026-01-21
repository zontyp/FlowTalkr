package workflow

data class WorkflowDefinition(
    val id:String,
    val nodes: List<NodeDefinition>,
    val triggers: List<TriggerDefinition> = emptyList()
)
