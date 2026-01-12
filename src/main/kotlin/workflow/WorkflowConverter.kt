package workflow

class WorkflowConverter {

    fun convert(definition: WorkflowDefinition): Workflow {
        val workflowNodes = definition.nodes.associate { defNode ->
            val node = NodeFactory.create(
                name = defNode.name,
                type = defNode.type,
                config = defNode.config
            )

            defNode.name to WorkflowNode(
                name = defNode.name,
                node = node,
                next = defNode.next
            )
        }

        return Workflow(
            start = definition.start,
            nodes = workflowNodes
        )
    }
}
