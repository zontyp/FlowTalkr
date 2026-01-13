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

    // Switch does not read or write workflow state
    override val stateAccess: StateAccess = StateAccess.STATELESS

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val fieldPath = configData["field"]?.asText()
            ?: error("SWITCH node missing 'field'")

        val valueNode = JsonPathResolver.resolve(fieldPath, inputData)
            ?: error("Path not found: $fieldPath")

        if (!valueNode.isTextual) {
            error("SWITCH field must resolve to a string")
        }

        // Preserve context, add routing hint
        val output = inputData.deepCopy<ObjectNode>()
        output.put("switch", valueNode.asText())

        return NodeResult(
            outputData = output
        )
    }
}
