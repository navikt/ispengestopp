package no.nav.syfo

import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.database.toList
import java.sql.ResultSet
import java.time.ZoneOffset
import java.util.*

const val queryStatusInsert =
    """INSERT INTO status_endring (
        id,
        uuid,
        sykmeldt_fnr,
        veileder_ident,
        status,
        virksomhet_nr,
        enhet_nr,
        opprettet) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, DEFAULT)"""

const val queryStatusRetrieve =
    """
    SELECT DISTINCT ON (sykmeldt_fnr, virksomhet_nr) *
    FROM status_endring
    WHERE sykmeldt_fnr = ?
    ORDER BY sykmeldt_fnr, virksomhet_nr, opprettet DESC
"""

fun DatabaseInterface.addStatus(fnr: SykmeldtFnr, ident: VeilederIdent, enhetNr: EnhetNr, virksomhetNr: VirksomhetNr) {
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

fun ResultSet.statusEndring(): StatusEndring =
    StatusEndring(
        VeilederIdent(getString("veileder_ident")),
        SykmeldtFnr(getString("sykmeldt_fnr")),
        Status.valueOf(getString("status")),
        VirksomhetNr(getString("virksomhet_nr")),
        getTimestamp("opprettet").toInstant().atOffset(ZoneOffset.UTC),
        EnhetNr(
            getString("enhet_nr")
        )
    )

fun DatabaseInterface.getActiveFlags(fnr: SykmeldtFnr): List<StatusEndring> {
    return connection.use { connection ->
        connection.prepareStatement(queryStatusRetrieve).use {
            it.setString(1, fnr.value)
            it.executeQuery().toList {
                statusEndring()
            }
        }
    }
}
