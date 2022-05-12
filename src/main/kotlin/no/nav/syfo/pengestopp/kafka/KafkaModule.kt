package no.nav.syfo.pengestopp.kafka

import kotlinx.coroutines.*
import no.nav.syfo.application.*
import no.nav.syfo.application.database.DatabaseInterface
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
