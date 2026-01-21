package engine

import kotlinx.coroutines.*

object ExecutionScope {
    val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )
}
