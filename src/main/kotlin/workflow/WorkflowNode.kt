//package workflow
//
//import engine.Node
///**
// * Node = executable behavior.
// * WorkflowNode = workflow structure + routing metadata.
// */
//data class WorkflowNode(
//    val name: String,
//    val node: Node,
//    /**
//     * Maps a routing key to the next node name.
//     *
//     * Common routing keys:
//     * - "default" → unconditional continuation
//     * - "true"    → IF condition evaluated to true
//     * - "false"   → IF condition evaluated to false
//     *
//     * If the map is empty or no key matches, the workflow ends at this node.
//     * Routing keys are selected by the Engine, not by the Node itself.
//     */
//)
