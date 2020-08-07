package no.nav.syfo

import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.config.bootstrapDBInit
import no.nav.syfo.database.VaultCredentialService
import no.nav.syfo.kafka.createPersonFlagget84Producer
import no.nav.syfo.util.getFileAsString
import no.nav.syfo.vault.RenewVaultService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.BootstrapKt")

@InternalAPI
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
    launchListeners(applicationState)
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
    applicationState: ApplicationState
) {
    createListener(applicationState) {
        applicationState.ready = true

        blockingApplicationLogic(applicationState)
    }
}

@KtorExperimentalAPI
suspend fun blockingApplicationLogic(
    applicationState: ApplicationState
) {
    while (applicationState.ready) {
        delay(100)
    }
}
