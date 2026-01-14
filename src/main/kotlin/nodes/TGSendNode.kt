package nodes

import engine.Node
import engine.NodeResult
import engine.StateAccess
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.HttpURLConnection
import java.net.URL

class TGSendNode(
    override val name: String,
    override val configData: JsonNode,
    private val botToken: String,
    private val mapper: ObjectMapper
) : Node {

    override val type: String = "TG_SEND"

    // Sending messages does not touch state
    override val stateAccess: StateAccess = StateAccess.STATELESS

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val chatId = inputData["chatId"]?.asLong()
            ?: error("TGSendNode requires chatId in inputData")

        val textTemplate = configData["text"]?.asText()
            ?: error("TGSendNode missing 'text' in config")

        val text = resolveText(textTemplate, inputData)

        sendMessage(chatId, text)

        // Context must be preserved
        return NodeResult(outputData = inputData)
    }

    private fun resolveText(template: String, input: JsonNode): String {
        if (!template.startsWith("$")) return template

        // Convert $.message → /message
        val pointer = template
            .removePrefix("$")
            .replace(".", "/")

        return input.at(pointer).asText("")
    }


    private fun sendMessage(chatId: Long, text: String) {
        val url =
            URL("https://api.telegram.org/bot$botToken/sendMessage")

        val payload = mapper.createObjectNode().apply {
            put("chat_id", chatId)
            put("text", text)
        }

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.use {
            it.write(mapper.writeValueAsBytes(payload))
        }

        if (conn.responseCode != 200) {
            error("Telegram sendMessage failed")
        }
    }
}
