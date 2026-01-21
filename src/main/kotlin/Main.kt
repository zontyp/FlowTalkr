import engine.Engine
import com.fasterxml.jackson.databind.ObjectMapper
import cron.CronRegistrar
import db.PostgresDataSource
import telegram.TelegramWebhookTrigger
import workflow.NodeFactory
import workflow.WorkflowParser
import workflow.WorkflowConverter
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

fun maintest() {
    val process = ProcessBuilder(
        "bin/wasmtime.exe",
        "wasm/habit_stats_wasm.wasm"
    )
        .redirectErrorStream(true)
        .start()

    val inputJson = """
        {
          "habitName": "Test",
          "stats": [
            { "date": "2026-01-01", "status": "done" },
            { "date": "2026-01-02", "status": "skipped" },
            { "date": "2026-01-03", "status": "done" }
          ]
        }
    """.trimIndent()

    // 🔑 THIS IS CRITICAL
    process.outputStream.use { os ->
        os.write(inputJson.toByteArray())
        os.flush()
    } // stdin CLOSED here

    process.waitFor(3, TimeUnit.SECONDS)

    val output = process.inputStream.bufferedReader().readText()

    println("=== OUTPUT ===")
    println(output)
}

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
    val workflowList = listOf(workflow)
    CronRegistrar.register(workflowList)
// Initial input (empty for now)
    val engine = Engine()
    val trigger = TelegramWebhookTrigger(
        botToken = "",
        publicBaseUrl = "https://devon-argillaceous-jenni.ngrok-free.dev",
        workflow = workflow,
        engine = engine,
        mapper = mapper
    )

    trigger.start()

}
