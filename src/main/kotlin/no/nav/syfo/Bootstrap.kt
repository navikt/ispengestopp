package no.nav.syfo

import com.google.gson.GsonBuilder
import io.ktor.util.*
import kotlinx.coroutines.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.config.bootstrapDBInit
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.database.VaultCredentialService
import no.nav.syfo.kafka.createPersonFlagget84Consumer
import no.nav.syfo.kafka.createPersonFlagget84Producer
import no.nav.syfo.util.OffsetDateTimeConverter
import no.nav.syfo.util.getFileAsString
import no.nav.syfo.vault.RenewVaultService
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.BootstrapKt")

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

    val applicationEngine = createApplicationEngine(
        applicationState,
        database,
        env,
        personFlagget84Producer
    )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()

    RenewVaultService(vaultCredentialService, applicationState).startRenewTasks()

    log.info("Hello from ispengestopp")
    launchListeners(
        applicationState,
        database,
        personFlagget84Consumer
    )
}

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

@KtorExperimentalAPI
fun launchListeners(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    personFlagget84Consumer: KafkaConsumer<String, String>
) {
    createListener(applicationState) {
        applicationState.ready = true

        blockingApplicationLogic(
            applicationState,
            database,
            personFlagget84Consumer
        )
    }
}

@KtorExperimentalAPI
suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    personFlagget84Consumer: KafkaConsumer<String, String>
) {
    val gson = GsonBuilder()
        .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeConverter())
        .create()

    while (applicationState.ready) {
        personFlagget84Consumer.poll(Duration.ofMillis(0)).forEach { consumerRecord ->
            val hendelse: StatusEndring =
                gson.fromJson(consumerRecord.value(), StatusEndring::class.java)
            database.addStatus(
                hendelse.sykmeldtFnr,
                hendelse.veilederIdent,
                hendelse.enhetNr,
                hendelse.virksomhetNr
            )
        }
        delay(100)
    }
}
