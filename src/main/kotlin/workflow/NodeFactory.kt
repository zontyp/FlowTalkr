package workflow
import engine.Node
import nodes.SetNode
import nodes.IfNode
import com.fasterxml.jackson.databind.JsonNode
import nodes.PGWriteNode
import nodes.SwitchNode
import javax.sql.DataSource

class NodeFactory(private val dataSource: DataSource){

    fun create(
        name: String,
        type: String,
        config: JsonNode
    ): Node {
        return when (type) {
            "SET" -> SetNode(name, config)
            "IF"  -> IfNode(name, config)
            "SWITCH" -> SwitchNode(name, config) // 👈 required
            "PG_WRITE" -> PGWriteNode(name, config, dataSource)
            else  -> error("Unknown node type: $type")
        }
    }
}
