package no.nav.syfo

import no.nav.syfo.database.DatabaseInterface
import java.util.*

const val queryStatusInsert = """INSERT INTO status_endring (
        id,
        uuid,
        sykmeldt_fnr,
        veileder_ident,
        status,
        virksomhet_nr,
        enhet_nr,
        opprettet) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, DEFAULT)"""

fun DatabaseInterface.addFlagg(fnr: SykmeldtFnr, ident: VeilederIdent, enhetNr: EnhetNr, virksomhetNr: VirksomhetNr) {
    val uuid = UUID.randomUUID().toString()
    connection.use { connection ->
        connection.prepareStatement(queryStatusInsert).use {
            it.setString(1, uuid)
            it.setString(2, fnr.value)
            it.setString(3, ident.value)
            it.setString(4, Status.STOPP_AUTOMATIKK.toString())
            it.setString(5, virksomhetNr.value)
            it.setString(6, enhetNr.value)
            it.execute()
        }
        connection.commit()
    }
}
