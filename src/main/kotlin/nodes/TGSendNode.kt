package nodes

import engine.Node
import engine.NodeResult
import engine.StateAccess
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import java.net.HttpURLConnection
import java.net.URL

class TGSendNode(
    override val name: String,
    override val configData: JsonNode,
    private val botToken: String,
    private val mapper: ObjectMapper
) : Node {

    override val type: String = "TG_SEND"
    override val stateAccess: StateAccess = StateAccess.STATELESS

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val chatId = inputData["chatId"]?.asLong()
            ?: error("TGSendNode requires chatId in inputData")

        val mode = configData["mode"]?.asText() ?: "SIMPLE"

        when (mode) {

            "MULTI_ITEM_SINGLE_MESSAGE" -> {
                sendMultiItemMessage(chatId, inputData)
            }

            else -> {
                val textTemplate = configData["text"]?.asText()
                    ?: error("TGSendNode missing 'text' in config")

                val text = resolveText(textTemplate, inputData)
                sendMessage(chatId, text, null)
            }
        }

        val nextNode = configData["next"]?.asText()
        return NodeResult(
            outputData = inputData,
            nextNode = nextNode
        )
    }

    // ------------------------------------------------------------------

    private fun sendMultiItemMessage(chatId: Long, inputData: JsonNode) {

        val listPath = configData["listPath"]?.asText()
            ?: error("TGSendNode missing 'listPath'")

        val headerText = configData["headerText"]?.asText() ?: ""

        val rowTemplate = configData["rowTemplate"]
            ?: error("TGSendNode missing 'rowTemplate'")

        val items = resolveList(listPath, inputData)

        val messageText = buildString {
            append(headerText)
            append("\n\n")
            items.forEachIndexed { index, item ->
                append("${index + 1}. ${item["habit_name"].asText()}\n")
            }
        }

        val inlineKeyboard = mapper.createArrayNode()

        items.forEach { item ->
            val row = mapper.createArrayNode()

            rowTemplate.forEach { btn ->
                val text = btn["text"].asText()
                    .replace("{habit_name}", item["habit_name"].asText())

                val callbackData = btn["callback_data"].asText()
                    .replace("{id}", item["id"].asText())

                val button = mapper.createObjectNode().apply {
                    put("text", text)
                    put("callback_data", callbackData)
                }

                row.add(button)
            }

            inlineKeyboard.add(row)
        }

        val replyMarkup = mapper.createObjectNode().apply {
            set<ArrayNode>("inline_keyboard", inlineKeyboard)
        }

        sendMessage(chatId, messageText, replyMarkup)
    }

    // ------------------------------------------------------------------

    private fun resolveList(path: String, input: JsonNode): List<JsonNode> {
        val pointer = path.removePrefix("$").replace(".", "/")
        val node = input.at(pointer)
        if (!node.isArray) return emptyList()
        return node.toList()
    }

    private fun resolveText(template: String, input: JsonNode): String {
        if (!template.startsWith("$")) return template
        val pointer = template.removePrefix("$").replace(".", "/")
        return input.at(pointer).asText("")
    }

    private fun sendMessage(
        chatId: Long,
        text: String,
        replyMarkup: JsonNode?
    ) {
        val url = URL("https://api.telegram.org/bot$botToken/sendMessage")

        val payload = mapper.createObjectNode().apply {
            put("chat_id", chatId)
            put("text", text)
            if (replyMarkup != null) {
                set<JsonNode>("reply_markup", replyMarkup)
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
            error("Telegram sendMessage failed: ${conn.responseCode}")
        }
    }
}
