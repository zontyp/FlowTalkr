package workflow
//converts list of nodes in workflowdefinition to map(nodeName:Node)
class WorkflowConverter(
    private val nodeFactory: NodeFactory
) {

    fun convert(definition: WorkflowDefinition): Workflow {
        val nodesNameMap = definition.nodes.associate { defNode ->

            val node = nodeFactory.create(
                name = defNode.name,
                type = defNode.type,
                config = defNode.config
            )

            defNode.name to node
        }

        return Workflow(
            id = definition.id,
            nodesNameMap = nodesNameMap,
            triggers = definition.triggers
        )
    }
}
