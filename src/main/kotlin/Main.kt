import engine.Engine
import com.fasterxml.jackson.databind.ObjectMapper
import db.PostgresDataSource
import telegram.TelegramWebhookTrigger
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
    val nodeFactory = NodeFactory(dataSource,mapper,"7792900880:AAGvtYGBCzK0Can3RJ5WEKVgfpRx5Ujy8as")
    val converter = WorkflowConverter(nodeFactory)

    val workflow = converter.convert(workflowDefinition)

// Initial input (empty for now)
    val engine = Engine()
    val trigger = TelegramWebhookTrigger(
        botToken = "7792900880:AAGvtYGBCzK0Can3RJ5WEKVgfpRx5Ujy8as",
        publicBaseUrl = "https://devon-argillaceous-jenni.ngrok-free.dev",
        workflow = workflow,
        engine = engine,
        mapper = mapper
    )

    trigger.start()


}
