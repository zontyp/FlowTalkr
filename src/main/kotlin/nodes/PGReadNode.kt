package nodes

import engine.Node
import engine.NodeResult
import engine.StateAccess
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import javax.sql.DataSource

class PGReadNode(
    override val name: String,
    override val configData: JsonNode,
    private val dataSource: DataSource,
    private val mapper: ObjectMapper
) : Node {

    override val type: String = "PG_READ"
    override val stateAccess: StateAccess = StateAccess.STATELESS

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val sql = configData["query"]?.asText()
            ?: error("PGReadNode '$name' missing 'query'")

        val resultMode = configData["resultMode"]?.asText() ?: "SINGLE"
        val resultKey = configData["resultKey"]?.asText() ?: "result"

        val paramRegex = Regex(":(\\w+)")
        val paramNames = paramRegex.findAll(sql)
            .map { it.groupValues[1] }
            .toList()

        val resolvedSql = paramRegex.replace(sql, "?")

        val output = (inputData as? ObjectNode)?.deepCopy()
            ?: mapper.createObjectNode()

        dataSource.connection.use { conn ->
            conn.prepareStatement(resolvedSql).use { ps ->

                var index = 1
                for (param in paramNames) {
                    val value = inputData[param]
                        ?: error("Missing input field for SQL param: $param")

                    when {
                        value.isLong || value.isInt ->
                            ps.setLong(index++, value.asLong())

                        value.isBoolean ->
                            ps.setBoolean(index++, value.asBoolean())

                        value.isFloatingPointNumber ->
                            ps.setDouble(index++, value.asDouble())

                        value.isTextual ->
                            ps.setString(index++, value.asText())

                        else ->
                            ps.setObject(index++, value.toString())
                    }
                }

                val rs = ps.executeQuery()

                when (resultMode.uppercase()) {

                    "LIST" -> {
                        val arrayNode = mapper.createArrayNode()

                        while (rs.next()) {
                            val meta = rs.metaData
                            val rowNode = mapper.createObjectNode()

                            for (i in 1..meta.columnCount) {
                                val columnName = meta.getColumnLabel(i)
                                val value = rs.getObject(i)
                                rowNode.set<JsonNode>(
                                    columnName,
                                    mapper.valueToTree(value)
                                )
                            }

                            arrayNode.add(rowNode)
                        }

                        output.set<JsonNode>(resultKey, arrayNode)
                    }

                    "SINGLE" -> {
                        if (rs.next()) {
                            val meta = rs.metaData
                            for (i in 1..meta.columnCount) {
                                val columnName = meta.getColumnLabel(i)
                                val value = rs.getObject(i)
                                output.set<JsonNode>(
                                    columnName,
                                    mapper.valueToTree(value)
                                )
                            }
                        }
                        else{

                        }
                    }

                    else -> error("Unsupported resultMode: $resultMode")
                }
            }
        }

        // ✅ NEW: node decides continuation
        val nextNode = configData["next"]?.asText()

        return NodeResult(
            outputData = output,
            nextNode = nextNode
        )
    }
}
