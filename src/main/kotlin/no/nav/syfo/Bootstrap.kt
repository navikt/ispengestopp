package no.nav.syfo

import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.clients.KafkaConsumers
import no.nav.syfo.database.Database
import no.nav.syfo.database.VaultCredentialService
import no.nav.syfo.persistence.handleReceivedMessage
import no.nav.syfo.util.getFileAsString
import no.nav.syfo.vault.RenewVaultService
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.ispengestopp")

@InternalAPI
@KtorExperimentalAPI
fun main() {
    val env = Environment()
    val vaultSecrets = VaultSecrets(
        serviceuserPassword = getFileAsString("/secrets/serviceuser/password"),
        serviceuserUsername = getFileAsString("/secrets/serviceuser/username")
    )

    val vaultCredentialService = VaultCredentialService()
    val database = Database(env, vaultCredentialService)

    val applicationState = ApplicationState()

    val kafkaConsumers = KafkaConsumers(env, vaultSecrets)

    val applicationEngine = createApplicationEngine(
        env,
        applicationState
    )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()

    applicationState.ready = true

    log.info("Hello from ispengestopp")

    if (!env.developmentMode) {
        RenewVaultService(vaultCredentialService, applicationState).startRenewTasks()
    }

    launchListeners(
        applicationState,
        database,
        env,
        kafkaConsumers
    )
}

@InternalAPI
fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (e: Exception) {
            log.error(
                "En uhåndtert feil oppstod, applikasjonen restarter {}",
                StructuredArguments.fields(e.message), e.cause
            )
        } finally {
            applicationState.alive = false
        }
    }

@InternalAPI
@KtorExperimentalAPI
fun launchListeners(
    applicationState: ApplicationState,
    database: Database,
    env: Environment,
    kafkaConsumers: KafkaConsumers
) {
    createListener(applicationState) {
        val kafkaConsumerSmReg = kafkaConsumers.kafkaConsumerSmReg

        applicationState.ready = true

        log.info("Subscribing to topics: ${env.kafkaConsumerTopics}")
        kafkaConsumerSmReg.subscribe(env.kafkaConsumerTopics)
        blockingApplicationLogic(
            applicationState,
            database,
            env,
            kafkaConsumerSmReg
        )
    }
}

@KtorExperimentalAPI
suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    database: Database,
    env: Environment,
    kafkaConsumer: KafkaConsumer<String, String>
) {
    while (applicationState.ready) {
        val endOffsets = endOffsetsOrEmptyMap(kafkaConsumer)
        kafkaConsumer.poll(Duration.ofMillis(0)).forEach { consumerRecord ->
            handleReceivedMessage(database, env, consumerRecord, endOffsets)
        }
        delay(100)
    }
}

fun endOffsetsOrEmptyMap(kafkaConsumer: KafkaConsumer<String, String>): Map<TopicPartition, Long> {
    return try {
        kafkaConsumer.endOffsets(kafkaConsumer.assignment())
    } catch (e: Exception) {
        log.info("Kafka-trace: Fikk feil ved henting av endOffsets, returnerer tom map", e.message)
        emptyMap()
    }
}
