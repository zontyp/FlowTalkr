package nodes

import engine.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode

class SetNode(
    override val name: String,
    override val configData: JsonNode
) : Node {

    override val type = "SET"
    override val stateAccess = StateAccess.STATELESS

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val merged = inputData.deepCopy<ObjectNode>()

        configData.fields().forEach { (targetKey, configValue) ->

            val result: JsonNode = when {
                // 🔹 NEW: object-based SET (from / op / arg)
                configValue.isObject -> {
                    applyOperation(configValue, merged)
                }

                // 🔹 OLD: literal copy (backward compatible)
                else -> configValue
            }

            merged.set<JsonNode>(targetKey, result)
        }

        val nextNode = configData["next"]?.asText()
        return NodeResult(
            outputData = merged,
            nextNode = nextNode
        )
    }

    // ------------------------------------------------------------

    private fun applyOperation(
        cfg: JsonNode,
        input: ObjectNode
    ): JsonNode {

        val fromPath = cfg["from"]?.asText()
            ?: error("SET node missing 'from'")

        val op = cfg["op"]?.asText()
            ?: error("SET node missing 'op'")

        val arg = cfg["arg"]?.asText()

        val sourceNode = resolve(fromPath, input)
            ?: return NullNode.instance

        val value = sourceNode.asText()

        return when (op) {

            "SPLIT_BEFORE" -> {
                if (arg != null && value.contains(arg))
                    TextNode(value.substringBefore(arg))
                else
                    TextNode(value) // no delimiter → keep original
            }

            "SPLIT_AFTER" -> {
                if (arg != null && value.contains(arg))
                    TextNode(value.substringAfter(arg))
                else
                    NullNode.instance
            }

            "PREFIX" -> {
                if (arg != null && value.contains(arg)) {
                    val idx = value.indexOf(arg)
                    TextNode(value.substring(0, idx + arg.length))
                } else {
                    NullNode.instance
                }


            }

            else -> error("Unsupported SET op: $op")
        }
    }

    private fun resolve(path: String, input: JsonNode): JsonNode? {
        val pointer = path.removePrefix("$").replace(".", "/")
        val node = input.at(pointer)
        return if (node.isMissingNode) null else node
    }
}
