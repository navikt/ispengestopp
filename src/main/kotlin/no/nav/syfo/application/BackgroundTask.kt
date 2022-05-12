package no.nav.syfo.application

import kotlinx.coroutines.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.log
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

fun launchBackgroundTask(
    applicationState: ApplicationState,
    action: suspend CoroutineScope.() -> Unit,
): Job =
    GlobalScope.launch(Dispatchers.Unbounded) {
        try {
            action()
        } catch (e: Exception) {
            log.error(
                "En uhåndtert feil oppstod, applikasjonen restarter {}",
                StructuredArguments.fields(e.message),
                e.cause
            )
        } finally {
            applicationState.alive = false
            applicationState.ready = false
        }
    }

/*
Use Dispatchers.Unbounded to allow unlimited number of coroutines to be dispatched. Without this
only a few will be allowed simultaneously (depending on the number of available cores) which may result
in cronjobs or Kafka-consumers not starting as intended.
*/
val Dispatchers.Unbounded get() = UnboundedDispatcher.unboundedDispatcher

class UnboundedDispatcher private constructor() : CoroutineDispatcher() {
    companion object {
        val unboundedDispatcher = UnboundedDispatcher()
    }

    private val threadPool = Executors.newCachedThreadPool()
    private val dispatcher = threadPool.asCoroutineDispatcher()
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatcher.dispatch(context, block)
    }
}
