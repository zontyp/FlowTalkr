package workflow
import engine.Node
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import nodes.*
import javax.sql.DataSource

class NodeFactory(
    private val dataSource: DataSource,
    private val mapper: ObjectMapper,
    private val botToken:String
    ){

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
            "PG_READ" -> PGReadNode(name, config, dataSource, mapper)
            "TG_SEND" -> TGSendNode(name, config, botToken, mapper)
            "WASM" -> WasmNode(name, config,mapper)



            else  -> error("Unknown node type: $type")
        }
    }
}
