package nodes

import engine.Node
import engine.NodeResult
import engine.StateAccess
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import javax.sql.DataSource

class PGWriteNode(
    override val name: String,
    override val configData: JsonNode,
    private val dataSource: DataSource
) : Node {

    override val type: String = "PG_WRITE"
    override val stateAccess: StateAccess = StateAccess.STATELESS

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val sql = configData["query"]?.asText()
            ?: error("PGWriteNode missing 'query'")

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->

                // Simple named parameters replacement (v1)
                // :userId → ?
                var index = 1
                // Convert named parameters (:param) → JDBC placeholders (?)
                val resolvedSql = Regex(":(\\w+)").replace(sql) { "?" }
                //userId
                val paramNames = Regex(":(\\w+)").findAll(sql).map { it.groupValues[1] }.toList()

                conn.prepareStatement(resolvedSql).use { ps ->
                    for (param in paramNames) {
                        val value = inputData[param]
                            ?: error("Missing input field for SQL param: $param")

                        when {
                            value.isLong || value.isInt ->
                                ps.setLong(index++, value.asLong())

                            value.isBoolean ->
                                ps.setBoolean(index++, value.asBoolean())

                            value.isDouble || value.isFloat ->
                                ps.setDouble(index++, value.asDouble())

                            value.isTextual ->
                                ps.setString(index++, value.asText())

                            else ->
                                ps.setObject(index++, value.toString())
                        }
                    }

                    ps.executeUpdate()
                }
            }
        }

        // Preserve context
        val output = inputData.deepCopy<ObjectNode>()

        return NodeResult(outputData = output)
    }
}
