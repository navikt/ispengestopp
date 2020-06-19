package no.nav.syfo.api.testutils

import no.nav.syfo.*
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.database.DbConfig
import no.nav.syfo.database.DevDatabase
import no.nav.syfo.database.toList
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime

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

const val queryStatusInsertTest = """INSERT INTO status_endring (
        id,
        uuid,
        sykmeldt_fnr,
        veileder_ident,
        status,
        virksomhet_nr,
        timestamptz) VALUES (?, ?, ?, ?, ?, ?, ?)"""

fun Connection.hentStatusEndringListe(sykmeldtFnr: SykmeldtFnr, virksomhetNr: VirksomhetNr): List<StatusEndring> {
    return use { connection ->
        connection.prepareStatement(queryStatusEndring).use {
            it.setString(1, sykmeldtFnr.value)
            it.setString(2, virksomhetNr.value)
            it.executeQuery().toList {
                toStatusEndring()
            }
        }
    }
}


// TODO Jeg har testet litt her, det kan sikkert være lurt å teste insert, men da må det gjøres litt mer ordentlig.
fun Connection.testInsert(sykmeldtFnr: SykmeldtFnr, veilederIdent: VeilederIdent, virksomhetNr: VirksomhetNr) {
    use { connection ->
        connection.prepareStatement(queryStatusInsertTest).use{
            it.setString(1, "id1")
            it.setString(2, "uuid1")
            it.setString(3, sykmeldtFnr.value)
            it.setString(4, veilederIdent.value)
            it.setString(5, Status.STOPP_AUTOMATIKK.toString())
            it.setString(6, virksomhetNr.value)
            it.setTimestamp(7, Timestamp.from(Instant.now()))
            it.execute()
        }
        connection.commit()
    }
}

fun ResultSet.toStatusEndring(): StatusEndring =
    StatusEndring(
        veilederIdent = VeilederIdent(getString("veileder_ident")),
        sykmeldtFnr = SykmeldtFnr(getString("sykmeldt_fnr")),
        status = Status.valueOf(getString("status")),
        virksomhetNr = VirksomhetNr(getString("virksomhet_nr")),
        date = getObject("timestamptz", Timestamp::class.java).toLocalDateTime() /* TODO Endret denne litt. Jeg er usikker på hva som er best, men nå har vi "TIMESTAMP" i tabellen, også plukker vi det ut og gjør om til LocalDateTime.
                                                                                             Sånn det så ut tidligere med Instant funket ikke, den klagde på at den ikke kunne gjøre Timestamp (eller date) til Instant. */
    )


fun Connection.dropData() {
    val query = "DELETE FROM status_endring"
    use { connection ->
        connection.prepareStatement(query).executeUpdate()
        connection.commit()
    }
}
