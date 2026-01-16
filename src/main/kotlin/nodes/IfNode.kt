package nodes

import engine.Node
import engine.NodeResult
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import engine.StateAccess

class IfNode(
    override val name: String,
    override val configData: JsonNode
) : Node {

    override val type = "SET"
    override val stateAccess = StateAccess.READS_STATE
    private val mapper = ObjectMapper()

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val leftPath = configData["left"]?.asText()
            ?: error("IF node missing 'left'")

        val operator = Operator.valueOf(
            configData["operator"]?.asText()
                ?: error("IF node missing 'operator'")
        )

        val rightValue = configData["right"]
            ?: error("IF node missing 'right'")

        val leftValue = JsonPathResolver.resolve(leftPath, inputData,"false")
            ?: error("Path not found: $leftPath")

        val condition = when (operator) {
            Operator.EQUALS -> leftValue == rightValue.asText()
            Operator.NOT_EQUALS -> leftValue != rightValue.asText()
        }

        val output = inputData.deepCopy<ObjectNode>()

        return NodeResult(
            outputData = output,
            routeKey = condition.toString()
        )
    }
}
