package no.nav.syfo

import no.nav.syfo.database.DatabaseInterface
import java.sql.Timestamp
import java.time.Instant
import java.util.*


const val queryStatusInsert = """INSERT INTO status_endring (
        id,
        uuid,
        sykmeldt_fnr,
        veileder_ident,
        status,
        virksomhet_nr,
        timestamptz) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)"""

fun DatabaseInterface.addFlagg(fnr: SykmeldtFnr, ident: VeilederIdent, virksomhetNr: VirksomhetNr) {
    val tidspunkt = Timestamp.from(Instant.now())
    val uuid = UUID.randomUUID().toString()

    connection.use { connection ->
        connection.prepareStatement(queryStatusInsert).use {
            it.setString(1, uuid)
            it.setString(3, fnr.value)
            it.setString(4, ident.value)
            it.setString(5, Status.STOPP_AUTOMATIKK.toString())
            it.setString(6, virksomhetNr.value)
            it.setTimestamp(7, Timestamp.from(Instant.now()))
            it.execute()
        }
    }
}
