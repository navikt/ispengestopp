package no.nav.syfo.api.testutils

import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.database.DbConfig
import no.nav.syfo.database.DevDatabase
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection

class TestDB : DatabaseInterface {

    private val container = PostgreSQLContainer<Nothing>("postgres").apply {
        withDatabaseName("db_test")
        withUsername("username")
        withPassword("password")
    }

    private var db: DatabaseInterface
    override val connection: Connection
        get() = db.connection.apply { autoCommit = false }

    init {
        container.start()
        db = DevDatabase(
            DbConfig(
                jdbcUrl = container.jdbcUrl,
                username = "username",
                password = "password",
                databaseName = "db_test"
            )
        )
    }

    fun stop() {
        container.stop()
    }
}

fun Connection.dropData() {
    val query = "DELETE FROM status_endring"
    use { connection ->
        connection.prepareStatement(query).executeUpdate()
        connection.commit()
    }
}
