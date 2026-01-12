package workflow

data class Workflow(
    val start: String,
    val nodes: Map<String, WorkflowNode>
)
