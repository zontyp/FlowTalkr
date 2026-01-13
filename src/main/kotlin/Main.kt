import engine.Engine
import com.fasterxml.jackson.databind.ObjectMapper
import db.PostgresDataSource
import workflow.NodeFactory
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
    val dataSource = PostgresDataSource.create()
    val nodeFactory = NodeFactory(dataSource)
    val converter = WorkflowConverter(nodeFactory)

    val workflow = converter.convert(workflowDefinition)

// Initial input (empty for now)
    val engine = Engine()
    val result = engine.run(
        workflow = workflow,
        initialInput = mapper.readTree(
            """
        {
          "userId": 125
        }
        """
        )
    )

    println(result.toPrettyString())
}
