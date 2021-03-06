package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.apiModule
import no.nav.syfo.application.authentication.getWellKnown
import no.nav.syfo.config.bootstrapDBInit
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.database.VaultCredentialService
import no.nav.syfo.kafka.createPersonFlagget84Consumer
import no.nav.syfo.kafka.createPersonFlagget84Producer
import no.nav.syfo.util.getFileAsString
import no.nav.syfo.util.pollAndPersist
import no.nav.syfo.vault.RenewVaultService
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerKotlinModule()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.BootstrapKt")

@InternalCoroutinesApi
@KtorExperimentalAPI
fun main() {
    val env = Environment()
    val vaultSecrets = VaultSecrets(
        serviceuserPassword = getFileAsString("/secrets/serviceuser/password"),
        serviceuserUsername = getFileAsString("/secrets/serviceuser/username")
    )
    val vaultCredentialService = VaultCredentialService()

    val applicationState = ApplicationState()
    val database = bootstrapDBInit(env, applicationState, vaultCredentialService)

    val personFlagget84Producer = createPersonFlagget84Producer(env, vaultSecrets)
    val personFlagget84Consumer = createPersonFlagget84Consumer(env, vaultSecrets)

    val wellKnown = getWellKnown(env.aadDiscoveryUrl)
    val wellKnownInternADV2 = getWellKnown(env.azureAppWellKnownUrl)

    val applicationEngine = embeddedServer(Netty, env.applicationPort) {
        apiModule(
            applicationState = applicationState,
            database = database,
            env = env,
            personFlagget84Producer = personFlagget84Producer,
            wellKnownInternADV1 = wellKnown,
            wellKnownInternADV2 = wellKnownInternADV2,
        )
    }

    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()

    RenewVaultService(vaultCredentialService, applicationState).startRenewTasks()

    log.info("Hello from ispengestopp")
    launchListeners(
        applicationState,
        database,
        personFlagget84Consumer,
        env
    )
}

fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
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
            applicationState.alive.set(false)
        }
    }

@InternalCoroutinesApi
@KtorExperimentalAPI
fun launchListeners(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    personFlagget84Consumer: KafkaConsumer<String, String>,
    env: Environment
) {
    createListener(applicationState) {
        applicationState.ready.set(true)

        blockingApplicationLogic(
            applicationState,
            database,
            personFlagget84Consumer,
            env
        )
    }
}

@KtorExperimentalAPI
suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    personFlagget84Consumer: KafkaConsumer<String, String>,
    env: Environment
) {
    while (applicationState.ready.get()) {
        pollAndPersist(personFlagget84Consumer, database, env)
        delay(100)
    }
}
