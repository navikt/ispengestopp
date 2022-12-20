package no.nav.syfo.pengestopp.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import no.nav.syfo.application.*
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.objectMapper
import no.nav.syfo.pengestopp.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.*

@InternalCoroutinesApi
fun launchKafkaTask(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    personFlagget84Consumer: KafkaConsumer<String, String>,
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

fun bootstrapAivenTopic(
    applicationState: ApplicationState,
    environment: Environment,
    personFlagget84Consumer: KafkaConsumer<String, String>,
    personFlagget84AivenProducer: KafkaProducer<String, StatusEndring>,
) {
    launchBackgroundTask(applicationState = applicationState) {
        while (applicationState.ready) {
            val records = personFlagget84Consumer.poll(Duration.ofMillis(environment.pollTimeOutMs))
            if (!records.isEmpty) {
                records.forEach { consumerRecord ->
                    val kFlaggperson84Hendelse: StatusEndring = objectMapper.readValue(consumerRecord.value())
                    personFlagget84AivenProducer.send(
                        ProducerRecord(
                            environment.stoppAutomatikkAivenTopic,
                            consumerRecord.key(),
                            kFlaggperson84Hendelse,
                        )
                    )
                }
            }
            delay(100)
        }
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
