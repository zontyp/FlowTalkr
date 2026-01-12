package nodes

import com.fasterxml.jackson.databind.JsonNode

object JsonPathResolver {

    fun resolve(path: String, input: JsonNode): JsonNode? {
        require(path.startsWith("$."))
        val parts = path.removePrefix("$.").split(".")

        var current: JsonNode? = input
        for (part in parts) {
            current = current?.get(part)
            if (current == null) return null
        }
        return current
    }
}
