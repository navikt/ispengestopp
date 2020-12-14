package no.nav.syfo.api.testutils

import no.nav.syfo.StatusEndring
import no.nav.syfo.SykmeldtFnr
import no.nav.syfo.VirksomhetNr
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.database.DbConfig
import no.nav.syfo.database.DevDatabase
import no.nav.syfo.database.domain.toStatusEndring
import no.nav.syfo.database.toList
import no.nav.syfo.statusEndring
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

const val queryStatusEndring =
    """
    SELECT * 
    FROM status_endring
    WHERE sykmeldt_fnr = ?
    AND virksomhet_nr = ?
"""

fun Connection.hentStatusEndringListe(sykmeldtFnr: SykmeldtFnr, virksomhetNr: VirksomhetNr): List<StatusEndring> {
    return use { connection ->
        connection.prepareStatement(queryStatusEndring).use {
            it.setString(1, sykmeldtFnr.value)
            it.setString(2, virksomhetNr.value)
            it.executeQuery().toList {
                statusEndring()
            }
        }
    }.map { it.toStatusEndring() }
}

fun Connection.dropData() {
    val query = "DELETE FROM status_endring"
    use { connection ->
        connection.prepareStatement(query).executeUpdate()
        connection.commit()
    }
}
