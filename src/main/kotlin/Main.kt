import engine.Engine
import nodes.SetNode
import com.fasterxml.jackson.databind.ObjectMapper
import workflow.WorkflowParser
import workflow.WorkflowConverter

fun main() {
    val mapper = ObjectMapper()

    val workflowStream = object {}.javaClass
        .getResourceAsStream("/workflows/StreakSetGo.json")
        ?: error("workflow not found")

    val workflowJson = mapper.readTree(workflowStream)


    val parser = WorkflowParser(mapper)
    val workflowDefinition = parser.parse(workflowJson)
    val workflow  = WorkflowConverter().convert(workflowDefinition)

// Initial input (empty for now)
    val initialInput = mapper.readTree("""{}""")
    val engine = Engine()
    val result = engine.run(
        workflow = workflow,
        initialInput = mapper.readTree("""{}""")
    )


    println(result.toPrettyString())
}
