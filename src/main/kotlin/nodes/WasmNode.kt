package nodes

import engine.Node
import engine.NodeResult
import engine.StateAccess
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

import io.github.kawamuray.wasmtime.Engine
import io.github.kawamuray.wasmtime.Module
import io.github.kawamuray.wasmtime.Store
import io.github.kawamuray.wasmtime.Linker
import io.github.kawamuray.wasmtime.Memory
import io.github.kawamuray.wasmtime.Func

class WASMNode(
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

        // ----------------------------
        // Config
        // ----------------------------

        val wasmFile = configData["wasmFile"]?.asText()
            ?: error("WASM node missing 'wasmFile'")

        val functionName = configData["function"]?.asText() ?: "run"

        val inputPath = configData["input"]?.asText() ?: "$"

        val outputKey = configData["outputKey"]?.asText()
            ?: error("WASM node missing 'outputKey'")

        val next = configData["next"]?.asText()

        // ----------------------------
        // Resolve input JSON
        // ----------------------------

        val wasmInput: JsonNode = if (inputPath == "$") {
            inputData
        } else {
            JsonPathResolver.resolveNode(inputPath, inputData)
                ?: error("Invalid input path: $inputPath")
        }

        val inputBytes = mapper.writeValueAsBytes(wasmInput)

        // ----------------------------
        // Load WASM from resources
        // ----------------------------

        val wasmBytes = javaClass.classLoader
            .getResourceAsStream(wasmFile)
            ?.readBytes()
            ?: error("WASM file not found in resources: $wasmFile")

        // ----------------------------
        // WASM execution
        // ----------------------------

        val engine = Engine()
        val module = Module(engine, wasmBytes)
        val store = Store<Void>(engine)
        val linker = Linker(engine)

        val instance = linker.instantiate(store, module)

        val memory: Memory = instance.getMemory(store, "memory")
            ?: error("WASM module must export memory")

        val run: Func = instance.getFunc(store, functionName)
            ?: error("WASM module must export function '$functionName'")

        // ----------------------------
        // Write input to WASM memory
        // ----------------------------

        val ptr = memory.dataSize(store).toInt()

        val pagesNeeded = ((inputBytes.size + 1) / 65536) + 1
        memory.grow(store, pagesNeeded)

        memory.write(store, ptr, inputBytes)
        memory.write(store, ptr + inputBytes.size, byteArrayOf(0))

        // ----------------------------
        // Call WASM function
        // ----------------------------

        val resultPtr = run.call(store, ptr) as Int

        // ----------------------------
        // Read null-terminated output
        // ----------------------------

        val mem = memory.data(store)
        var end = resultPtr
        while (mem[end] != 0.toByte()) {
            end++
        }

        val outputText = String(mem, resultPtr, end - resultPtr)

        // ----------------------------
        // Merge output into workflow data
        // ----------------------------

        val output = inputData.deepCopy<ObjectNode>()
        output.put(outputKey, outputText)

        return NodeResult(
            outputData = output,
            nextNode = next
        )
    }
}
