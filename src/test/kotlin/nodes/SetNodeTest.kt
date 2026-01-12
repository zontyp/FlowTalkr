package nodes

import engine.Engine
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class SetNodeTest {

    private val mapper = ObjectMapper()

    @Test
    fun `set node adds fields to output data`() {
        val engine = Engine()

        val input = mapper.readTree("""{ "text": "hello" }""")

        val node = SetNode(
            name = "Set Message",
            configData = mapper.readTree("""{ "message": "Hi" }""")
        )

        val result = engine.run(
            nodes = listOf(node),
            initialInput = input
        )

        assertEquals(
            mapper.readTree("""{ "text": "hello", "message": "Hi" }"""),
            result
        )
    }
}
