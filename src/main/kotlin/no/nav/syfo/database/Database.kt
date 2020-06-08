package no.nav.syfo.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.Environment
import no.nav.syfo.log
import org.flywaydb.core.Flyway
import java.sql.Connection

enum class Role {
    ADMIN, USER, READONLY;

    override fun toString() = name.toLowerCase()
}

class Database(private val env: Environment, private val vaultCredentialService: VaultCredentialService) :
    DatabaseInterface {
    private val dataSource: HikariDataSource

    override val connection: Connection
        get() = dataSource.connection

    init {
        runFlywayMigrations()

        val initialCredentials = vaultCredentialService.getNewCredentials(
            mountPath = env.databaseMountPathVault,
            databaseName = env.databaseName,
            role = Role.USER
        )
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = env.ispengestoppDBURL
            username = initialCredentials.username
            password = initialCredentials.password
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            maxLifetime = 300000
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        })

        vaultCredentialService.renewCredentialsTaskData = RenewCredentialsTaskData(
            dataSource = dataSource,
            mountPath = env.databaseMountPathVault,
            databaseName = env.databaseName,
            role = Role.USER
        )
    }

    private fun runFlywayMigrations() = Flyway.configure().run {
        log.info("Run flyway migrations")
        val credentials = vaultCredentialService.getNewCredentials(
            mountPath = env.databaseMountPathVault,
            databaseName = env.databaseName,
            role = Role.ADMIN
        )
        dataSource(env.ispengestoppDBURL, credentials.username, credentials.password)
        // required for assigning proper owners for the tables
        initSql("SET ROLE \"${env.databaseName}-${Role.ADMIN}\"")
        load().migrate()
    }
}

interface DatabaseInterface {
    val connection: Connection
}
