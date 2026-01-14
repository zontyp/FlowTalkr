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

    // DB reads do not touch workflow state
    override val stateAccess: StateAccess = StateAccess.STATELESS

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val sql = configData["query"]?.asText()
            ?: error("PGReadNode missing 'query'")

        val paramRegex = Regex(":(\\w+)")
        val paramNames = paramRegex.findAll(sql)
            .map { it.groupValues[1] }
            .toList()

        val resolvedSql = paramRegex.replace(sql, "?")

        val output = inputData.deepCopy<ObjectNode>()

        dataSource.connection.use { conn ->
            conn.prepareStatement(resolvedSql).use { ps ->
                var index = 1

                // Bind parameters with correct typing
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

                val rs = ps.executeQuery()

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
            }
        }
        return NodeResult(outputData = output)
    }
}
