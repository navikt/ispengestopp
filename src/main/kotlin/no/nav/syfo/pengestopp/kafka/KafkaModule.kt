package no.nav.syfo.pengestopp.kafka

import kotlinx.coroutines.*
import no.nav.syfo.application.*
import no.nav.syfo.pengestopp.*
import org.apache.kafka.clients.consumer.KafkaConsumer

@InternalCoroutinesApi
fun launchKafkaTask(
    applicationState: ApplicationState,
    pengestoppRepository: IPengestoppRepository,
    environment: Environment,
    personFlagget84Consumer: KafkaConsumer<String, String>,
) {
    launchBackgroundTask(applicationState = applicationState) {
        blockingApplicationLogic(
            applicationState = applicationState,
            environment = environment,
            personFlagget84Consumer = personFlagget84Consumer,
            pengestoppRepository = pengestoppRepository,
        )
    }
}

suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    environment: Environment,
    personFlagget84Consumer: KafkaConsumer<String, String>,
    pengestoppRepository: IPengestoppRepository
) {
    while (applicationState.ready) {
        pollAndPersist(
            consumer = personFlagget84Consumer,
            repository = pengestoppRepository,
            env = environment,
        )
        delay(100)
    }
}
