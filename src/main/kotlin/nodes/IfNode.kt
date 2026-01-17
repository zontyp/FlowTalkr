package nodes

import engine.Node
import engine.NodeResult
import engine.StateAccess
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

class IfNode(
    override val name: String,
    override val configData: JsonNode
) : Node {

    override val type: String = "IF"
    override val stateAccess: StateAccess = StateAccess.READS_STATE

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val leftPath = configData["left"]?.asText()
            ?: error("IF node '$name' missing 'left'")

        val operator = Operator.valueOf(
            configData["operator"]?.asText()
                ?: error("IF node '$name' missing 'operator'")
        )

        val rightValue = configData["right"]?.asText()
            ?: error("IF node '$name' missing 'right'")

        val trueNext = configData["trueNext"]?.asText()
            ?: error("IF node '$name' missing 'trueNext'")

        val falseNext = configData["falseNext"]?.asText()
            ?: error("IF node '$name' missing 'falseNext'")

        val leftValue = JsonPathResolver
            .resolve(leftPath, inputData, "false")
            ?: error("Path not found: $leftPath")

        val condition = when (operator) {
            Operator.EQUALS -> leftValue == rightValue
            Operator.NOT_EQUALS -> leftValue != rightValue
        }

        val output = inputData.deepCopy<ObjectNode>()

        return NodeResult(
            outputData = output,
            nextNode = if (condition) trueNext else falseNext
        )
    }
}
