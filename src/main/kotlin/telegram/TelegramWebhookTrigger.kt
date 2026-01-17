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
        val url = URL("https://api.telegram.org/bot$botToken/setWebhook")

        val payload = mapper.createObjectNode().apply {
            put("url", "$publicBaseUrl$webhookPath")
            putArray("allowed_updates").apply {
                add("message")
                add("callback_query")
            }
        }

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        conn.outputStream.use {
            it.write(mapper.writeValueAsBytes(payload))
        }

        if (conn.responseCode != 200) {
            error("Failed to register Telegram webhook")
        }

        println("Telegram webhook registered with callback_query support")
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

        println("RAW TELEGRAM UPDATE: starts")
        println(update.toPrettyString())
        println("RAW TELEGRAM UPDATE: ends")

        val input = mapper.createObjectNode()

        when {
            // -------- TEXT MESSAGE --------
            update.has("message") -> {
                val message = update["message"]

                input.put("chatId", message["chat"]["id"].asLong())
                input.put("userId", message["from"]["id"].asLong())
                input.put(
                    "command",
                    message["text"]?.asText()?.removePrefix("/") ?: ""
                )
            }

            // -------- INLINE BUTTON CLICK --------
            update.has("callback_query") -> {
                val callback = update["callback_query"]

                input.put(
                    "chatId",
                    callback["message"]["chat"]["id"].asLong()
                )
                input.put(
                    "userId",
                    callback["from"]["id"].asLong()
                )
                input.put(
                    "command",
                    callback["data"].asText()
                )

                // REQUIRED by Telegram UX
                answerCallbackQuery(callback["id"].asText())
            }

            else -> {
                println("Ignored update type")
                return
            }
        }

        println("NORMALIZED INPUT TO ENGINE:")
        println(input.toPrettyString())

        val result = engine.run(
            workflow = workflow,
            initialInput = input
        )

        println("ENGINE RESULT:")
        println(result.toPrettyString())
    }
    private fun answerCallbackQuery(callbackQueryId: String) {
        val url = URL("https://api.telegram.org/bot$botToken/answerCallbackQuery")

        val payload = mapper.createObjectNode().apply {
            put("callback_query_id", callbackQueryId)
        }

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        conn.outputStream.use {
            it.write(mapper.writeValueAsBytes(payload))
        }
    }


}
