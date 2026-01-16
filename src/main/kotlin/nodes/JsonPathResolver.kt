package nodes

import com.fasterxml.jackson.databind.JsonNode

object JsonPathResolver {

    fun resolve(
        path: String,
        input: JsonNode,
        default: String = "default"
    ): String {
        require(path.startsWith("$."))

        val parts = path.removePrefix("$.").split(".")

        var current: JsonNode? = input
        for (part in parts) {
            current = current?.get(part)
            if (current == null || current.isNull) {
                return default
            }
        }

        return when {
            current!!.isTextual -> current.asText()
            current!!.isNumber -> current.asText()   // safe conversion
            current!!.isBoolean -> current.asText()
            else -> default
        }
    }
}
