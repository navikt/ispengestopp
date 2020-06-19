package no.nav.syfo

import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.database.*
import no.nav.syfo.util.getFileAsString
import no.nav.syfo.vault.RenewVaultService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.ispengestopp")

@InternalAPI
@KtorExperimentalAPI
fun main() {
    val env = Environment()
    val vaultSecrets = VaultSecrets(
        serviceuserPassword = getFileAsString("/secrets/serviceuser/password"),
        serviceuserUsername = getFileAsString("/secrets/serviceuser/username")
    )
    lateinit var database: DatabaseInterface

    val vaultCredentialService = VaultCredentialService()

    val applicationState = ApplicationState()

    val applicationEngine = createApplicationEngine(
        applicationState,
        database,
        env
    )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()

    if (env.developmentMode) {
        database = DevDatabase(
            DbConfig(
            jdbcUrl = "jdbc:postgresql://localhost:5432/ispengestoppsrv_dev",
            databaseName = "ispengestoppsrv_dev",
            password = "password",
            username = "username")
        )

        applicationState.ready = true
    }


    if (!env.developmentMode) {
        val newCredentials = vaultCredentialService.getNewCredentials(env.databaseMountPathVault, env.databaseName, Role.USER)

        database = ProdDatabase(DbConfig(
            jdbcUrl = env.ispengestoppDBURL,
            username = newCredentials.username,
            password = newCredentials.password,
            databaseName = env.databaseName,
            runMigrationsOninit = false)) { prodDatabase ->

            // i prod må vi kjøre flyway migrations med et eget sett brukernavn/passord
            vaultCredentialService.getNewCredentials(env.databaseMountPathVault, env.databaseName, Role.ADMIN).let {
                prodDatabase.runFlywayMigrations(env.ispengestoppDBURL, it.username, it.password)
            }

            vaultCredentialService.renewCredentialsTaskData = RenewCredentialsTaskData(env.databaseMountPathVault, env.databaseName, Role.USER) {
                prodDatabase.updateCredentials(username = it.username, password = it.password)
            }


            applicationState.ready = true
        }
    }

    applicationState.ready = true

    log.info("Hello from ispengestopp")

    if (!env.developmentMode) {
        RenewVaultService(vaultCredentialService, applicationState).startRenewTasks()
    }

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
    }
}
