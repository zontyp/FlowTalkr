package workflow

import engine.Node
import nodes.SetNode
import nodes.IfNode
import com.fasterxml.jackson.databind.JsonNode

object NodeFactory {

    fun create(
        name: String,
        type: String,
        config: JsonNode
    ): Node {
        return when (type) {
            "SET" -> SetNode(name, config)
            "IF"  -> IfNode(name, config)
            else  -> error("Unknown node type: $type")
        }
    }
}
