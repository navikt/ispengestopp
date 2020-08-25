package no.nav.syfo.api.testutils

import no.nav.syfo.*
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.database.DbConfig
import no.nav.syfo.database.DevDatabase
import no.nav.syfo.database.toList
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TestDB : DatabaseInterface {

    val container = PostgreSQLContainer<Nothing>("postgres").apply {
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

const val queryStatusEndring = """
    SELECT * 
    FROM status_endring
    WHERE sykmeldt_fnr = ?
    AND virksomhet_nr = ?
"""

const val queryStatusAdd = """INSERT INTO status_endring (
        id,
        uuid,
        sykmeldt_fnr,
        veileder_ident,
        status,
        virksomhet_nr,
        enhet_nr,
        opprettet) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?)"""


fun Connection.hentStatusEndringListe(sykmeldtFnr: SykmeldtFnr, virksomhetNr: VirksomhetNr): List<StatusEndring> {
    return use { connection ->
        connection.prepareStatement(queryStatusEndring).use {
            it.setString(1, sykmeldtFnr.value)
            it.setString(2, virksomhetNr.value)
            it.executeQuery().toList {
                statusEndring()
            }
        }
    }
}

fun Connection.addStatus(dbStatusChangeTest: DBStatusChangeTest) {
    use { connection ->
        connection.prepareStatement(queryStatusAdd).use {
            it.setString(1, dbStatusChangeTest.uuid)
            it.setString(2, dbStatusChangeTest.sykmeldtFnr.value)
            it.setString(3, dbStatusChangeTest.veilederIdent.value)
            it.setString(4, dbStatusChangeTest.status.toString())
            it.setString(5, dbStatusChangeTest.virksomhetNr.value)
            it.setString(6, dbStatusChangeTest.enhetNr.value)
            it.setObject(7, dbStatusChangeTest.opprettet)
            it.execute()
        }
        connection.commit()
    }
}

fun ResultSet.statusEndring(): StatusEndring =
        StatusEndring(
                VeilederIdent(getString("veileder_ident")),
                SykmeldtFnr(getString("sykmeldt_fnr")),
                Status.valueOf(getString("status")),
                VirksomhetNr(getString("virksomhet_nr")),
                OffsetDateTime.ofInstant(getTimestamp("opprettet").toInstant(), ZoneOffset.UTC),
                EnhetNr(getString("enhet_nr"))
        )


fun Connection.dropData() {
    val query = "DELETE FROM status_endring"
    use { connection ->
        connection.prepareStatement(query).executeUpdate()
        connection.commit()
    }
}
