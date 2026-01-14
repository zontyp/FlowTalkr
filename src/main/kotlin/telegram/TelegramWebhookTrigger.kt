package telegram

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import engine.Engine
import workflow.Workflow
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import java.net.HttpURLConnection
import java.net.URL

class TelegramWebhookTrigger(
    private val botToken: String,
    private val publicBaseUrl: String,
    private val workflow: Workflow,
    private val engine: Engine,
    private val mapper: ObjectMapper
) {

    private val webhookPath = "/webhook/telegram"

    fun start() {
        registerWebhook()
        startServer()
    }

    /**
     * Tells Telegram where to send updates
     */
    private fun registerWebhook() {
        val url =
            "https://api.telegram.org/bot$botToken/setWebhook?url=$publicBaseUrl$webhookPath"

        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"

        if (conn.responseCode != 200) {
            error("Failed to register Telegram webhook")
        }

        println("Telegram webhook registered at $publicBaseUrl$webhookPath")
    }

    /**
     * Starts HTTP server to receive Telegram updates
     */
    private fun startServer() {
        embeddedServer(Netty, port = 8080) {
            routing {
                post(webhookPath) {
                    val payload = call.receiveText()
                    val update = mapper.readTree(payload)

                    handleUpdate(update)

                    call.respond(HttpStatusCode.OK)
                }
            }
        }.start(wait = true)
    }

    /**
     * Converts Telegram update → workflow input
     */
    private fun handleUpdate(update: JsonNode) {
        val message = update["message"] ?: return

        val input = mapper.createObjectNode().apply {
            put("chatId", message["chat"]["id"].asLong())
            put("userId", message["from"]["id"].asLong())
            put("text", message["text"]?.asText() ?: "")
            put(
                "command",
                message["text"]?.asText()?.removePrefix("/") ?: "text"
            )
        }
        println(input.toPrettyString())
        val result = engine.run(
            workflow = workflow,
            initialInput = input
        )
        println(result.toPrettyString())
    }
}
