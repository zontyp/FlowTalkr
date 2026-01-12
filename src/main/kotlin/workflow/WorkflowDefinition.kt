package workflow

data class WorkflowDefinition(
    val start: String,
    val nodes: List<NodeDefinition>
)
