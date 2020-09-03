package no.nav.syfo.config

import no.nav.syfo.Environment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.database.*

fun bootstrapDBInit(env: Environment, applicationState: ApplicationState, vaultCredentialService: VaultCredentialService): DatabaseInterface {
    val newCredentials = vaultCredentialService.getNewCredentials(env.databaseMountPathVault, env.databaseName, Role.USER)

    return ProdDatabase(DbConfig(
        jdbcUrl = env.ispengestoppDBURL,
        username = newCredentials.username,
        password = newCredentials.password,
        databaseName = env.databaseName,
        runMigrationsOninit = false)) { prodDatabase ->

        // i prod må vi kjøre flyway migrations med et eget sett brukernavn/passord
        vaultCredentialService.getNewCredentials(env.databaseMountPathVault, env.databaseName, Role.ADMIN).let {
            prodDatabase.runFlywayMigrations(env.ispengestoppDBURL, it.username, it.password)
        }

        vaultCredentialService.renewCredentialsTaskData =
            RenewCredentialsTaskData(env.databaseMountPathVault, env.databaseName, Role.USER) {
                prodDatabase.updateCredentials(username = it.username, password = it.password)
            }

        applicationState.ready.set(true)
    }
}
