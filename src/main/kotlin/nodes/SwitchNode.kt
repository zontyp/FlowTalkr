package nodes

import engine.Node
import engine.NodeResult
import engine.StateAccess
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

class SwitchNode(
    override val name: String,
    override val configData: JsonNode
) : Node {

    override val type: String = "SWITCH"
    override val stateAccess: StateAccess = StateAccess.STATELESS

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val cases = configData["cases"]
            ?: error("SWITCH node '$name' missing 'cases'")

        val output = inputData.deepCopy<ObjectNode>()

        for (caseNode in cases) {

            val leftPath = caseNode["left"]?.asText()
                ?: error("SWITCH case missing 'left'")

            val op = caseNode["op"]?.asText()
                ?: error("SWITCH case missing 'op'")

            val right = caseNode["right"]?.asText()
                ?: error("SWITCH case missing 'right'")

            val nextNode = caseNode["next"]?.asText()
                ?: error("SWITCH case missing 'next'")

            val leftValue = JsonPathResolver
                .resolve(leftPath, inputData)
                ?.toString()
                ?.trim('"')
                ?: ""

            if (matches(leftValue, op, right)) {
                return NodeResult(
                    outputData = output,
                    nextNode = nextNode
                )
            }
        }

        val defaultNext = configData["default"]?.asText()
            ?: error("SWITCH node '$name' missing 'default'")

        return NodeResult(
            outputData = output,
            nextNode = defaultNext
        )
    }

    // ------------------------------------------------

    private fun matches(left: String, op: String, right: String): Boolean {
        return when (op) {
            "EQUALS" -> left == right
            "STARTS_WITH" -> left.startsWith(right)
            "ENDS_WITH" -> left.endsWith(right)
            "CONTAINS" -> left.contains(right)
            "MATCHES_REGEX" -> Regex(right).containsMatchIn(left)
            else -> error("Unsupported SWITCH operator: $op")
        }
    }
}
