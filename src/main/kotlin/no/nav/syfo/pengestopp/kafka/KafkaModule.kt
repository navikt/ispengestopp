package no.nav.syfo.pengestopp.kafka

import kotlinx.coroutines.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.log
import no.nav.syfo.pengestopp.pollAndPersist
import org.apache.kafka.clients.consumer.KafkaConsumer

@InternalCoroutinesApi
fun launchKafkaTask(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    personFlagget84Consumer: KafkaConsumer<String, String>
) {
    launchBackgroundTask(applicationState = applicationState) {
        blockingApplicationLogic(
            applicationState = applicationState,
            database = database,
            environment = environment,
            personFlagget84Consumer = personFlagget84Consumer,
        )
    }
}

fun launchBackgroundTask(
    applicationState: ApplicationState,
    action: suspend CoroutineScope.() -> Unit,
): Job =
    GlobalScope.launch {
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

suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    personFlagget84Consumer: KafkaConsumer<String, String>
) {
    while (applicationState.ready) {
        pollAndPersist(
            consumer = personFlagget84Consumer,
            database = database,
            env = environment,
        )
        delay(100)
    }
}
