package nodes

import engine.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

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

        val merged = (inputData.deepCopy() as ObjectNode)

        configData.fields().forEach { (key, value) ->
            merged.set<JsonNode>(key, value)
        }

        return NodeResult(
            outputData = merged
        )
    }
}
