package no.nav.syfo.pengestopp.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.pengestopp.*
import no.nav.syfo.pengestopp.database.domain.*
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

const val queryInsertArsak =
    """INSERT INTO ARSAK (
        id,
        uuid,
        status_endring_id,
        arsaktype,
        opprettet) VALUES (DEFAULT, ?, ?, ?, ?)"""

const val queryStatusInsert =
    """INSERT INTO status_endring (
        id,
        uuid,
        sykmeldt_fnr,
        veileder_ident,
        status,
        virksomhet_nr,
        enhet_nr,
        opprettet) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?) RETURNING id"""

const val queryStatusRetrieve =
    """
    SELECT *
    FROM status_endring
    WHERE sykmeldt_fnr = ?
    ORDER BY opprettet DESC
"""

fun DatabaseInterface.addStatus(
    uuid: String,
    fnr: SykmeldtFnr,
    ident: VeilederIdent,
    enhetNr: EnhetNr,
    arsakList: List<Arsak>?,
    virksomhetNr: VirksomhetNr,
    opprettet: OffsetDateTime,
) {
    connection.use { connection ->
        val statusEndringId = connection.prepareStatement(queryStatusInsert).use {
            it.setString(1, uuid)
            it.setString(2, fnr.value)
            it.setString(3, ident.value)
            it.setString(4, Status.STOPP_AUTOMATIKK.toString())
            it.setString(5, virksomhetNr.value)
            it.setString(6, enhetNr.value)
            it.setTimestamp(7, Timestamp.from(opprettet.toInstant()))
            it.executeQuery().toList { getInt("id") }.first()
        }
        arsakList?.forEach { arsak ->
            connection.prepareStatement(queryInsertArsak).use {
                it.setString(1, UUID.randomUUID().toString())
                it.setInt(2, statusEndringId)
                it.setString(3, arsak.type.toString())
                it.setTimestamp(4, Timestamp.from(opprettet.toInstant()))
                it.execute()
            }
        }
        connection.commit()
    }
}

const val queryArsakListForStatusEndring =
    """
    SELECT * 
    FROM arsak
    WHERE status_endring_id = ?
"""

fun DatabaseInterface.getArsakListForStatusEndring(statusEndringId: Int): List<PArsak> {
    return connection.use { connection ->
        connection.prepareStatement(queryArsakListForStatusEndring).use {
            it.setInt(1, statusEndringId)
            it.executeQuery().toList { toPArsak() }
        }
    }
}

fun ResultSet.toPArsak(): PArsak =
    PArsak(
        id = getInt("id"),
        uuid = getString("uuid"),
        statusEndringId = getInt("status_endring_id"),
        arsakType = getString("arsaktype"),
        opprettet = getTimestamp("opprettet").toInstant().atOffset(ZoneOffset.UTC)
    )

fun ResultSet.statusEndring(): PStatusEndring =
    PStatusEndring(
        id = getInt("id"),
        uuid = getString("uuid"),
        veilederIdent = VeilederIdent(getString("veileder_ident")),
        sykmeldtFnr = SykmeldtFnr(getString("sykmeldt_fnr")),
        status = Status.valueOf(getString("status")),
        virksomhetNr = VirksomhetNr(getString("virksomhet_nr")),
        opprettet = getTimestamp("opprettet").toInstant().atOffset(ZoneOffset.UTC),
        enhetNr = EnhetNr(getString("enhet_nr"))
    )

fun DatabaseInterface.getActiveFlags(fnr: SykmeldtFnr): List<StatusEndring> {
    val statusEndringList = connection.use { connection ->
        connection.prepareStatement(queryStatusRetrieve).use {
            it.setString(1, fnr.value)
            it.executeQuery().toList {
                statusEndring()
            }
        }
    }
    return statusEndringList.map {
        val arsakList = getArsakListForStatusEndring(it.id).map { pArsak ->
            pArsak.toArsak()
        }
        it.toStatusEndring().copy(
            arsakList = arsakList
        )
    }
}

const val queryStatusEndringListForUUID =
    """
    SELECT *
    FROM status_endring
    WHERE uuid = ?
"""

fun DatabaseInterface.getActiveFlags(uuid: UUID): List<StatusEndring> {
    return connection.use { connection ->
        connection.prepareStatement(queryStatusEndringListForUUID).use {
            it.setString(1, uuid.toString())
            it.executeQuery().toList {
                statusEndring()
            }
        }
    }.map { it.toStatusEndring() }
}
