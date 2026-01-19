package nodes

import engine.Node
import engine.NodeResult
import engine.StateAccess
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.concurrent.TimeUnit

class WasmNode(
    override val name: String,
    override val configData: JsonNode,
    private val mapper: ObjectMapper
) : Node {

    override val type: String = "WASM"
    override val stateAccess: StateAccess = StateAccess.STATELESS

    override fun execute(
        inputData: JsonNode,
        state: Map<String, JsonNode>
    ): NodeResult {

        val wasmPath = configData["wasmPath"]?.asText()
            ?: error("WASM node missing 'wasmPath'")

        val outputKey = configData["outputKey"]?.asText()
            ?: "wasmOutput"

        val nextNode = configData["next"]?.asText()

        val jsonInput = mapper.writeValueAsString(inputData)

        val process = ProcessBuilder(
            "bin/wasmtime.exe",
            wasmPath
        )
            .redirectErrorStream(true)
            .start()

        // 🔑 CRITICAL: write stdin AND CLOSE IT
        process.outputStream.use { os ->
            os.write(jsonInput.toByteArray())
            os.flush()
        }

        // safety timeout
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("WASM execution timed out")
        }

        val stdout = process.inputStream
            .bufferedReader()
            .readText()
            .trim()

        if (stdout.isEmpty()) {
            error("WASM returned empty output")
        }

        val output = inputData.deepCopy<ObjectNode>()
        output.put(outputKey, stdout)

        return NodeResult(
            outputData = output,
            nextNode = nextNode
        )
    }
}
