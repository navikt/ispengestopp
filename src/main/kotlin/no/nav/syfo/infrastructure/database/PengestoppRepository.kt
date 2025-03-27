package no.nav.syfo.infrastructure.database

import no.nav.syfo.application.IPengestoppRepository
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.*
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.ZoneOffset
import java.util.*

class PengestoppRepository(private val database: DatabaseInterface) : IPengestoppRepository {
    override fun createStatusEndring(statusEndring: StatusEndring) = database.connection.use { connection ->
        connection.createStatusEndring(statusEndring)
        connection.commit()
    }

    override fun createStatusEndringer(statusEndringer: List<StatusEndring>) = database.connection.use { connection ->
        statusEndringer.forEach { statusEndring ->
            connection.createStatusEndring(statusEndring)
        }
        connection.commit()
    }

    private fun Connection.createStatusEndring(statusEndring: StatusEndring) {
        val statusEndringId = this.prepareStatement(INSERT_STATUS_ENDRING).use {
            it.setString(1, statusEndring.uuid)
            it.setString(2, statusEndring.sykmeldtFnr.value)
            it.setString(3, statusEndring.veilederIdent.value)
            it.setString(4, statusEndring.status.name)
            it.setObject(5, statusEndring.virksomhetNr?.value)
            it.setObject(6, statusEndring.enhetNr?.value)
            it.setObject(7, Timestamp.from(statusEndring.opprettet.toInstant()))
            it.executeQuery().toList { getInt("id") }.single()
        }

        statusEndring.arsakList.forEach { arsak ->
            this.prepareStatement(INSERT_ARSAK).use {
                it.setString(1, UUID.randomUUID().toString())
                it.setInt(2, statusEndringId)
                it.setString(3, arsak.type.name)
                it.setObject(4, Timestamp.from(statusEndring.opprettet.toInstant()))
                it.execute()
            }
        }
    }

    override fun getStatusEndringer(personIdent: PersonIdent): List<StatusEndring> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_STATUS_ENDRING_BY_PERSONIDENT).use {
                it.setString(1, personIdent.value)
                it.executeQuery().toList { statusEndring() }.map { pStatusEndring ->
                    pStatusEndring.toStatusEndring(arsaker = connection.getArsaker(pStatusEndring.id))
                }
            }
        }

    override fun getStatusEndring(uuid: UUID): StatusEndring? = database.connection.use { connection ->
        connection.prepareStatement(GET_STATUS_ENDRING_BY_UUID).use {
            it.setString(1, uuid.toString())
            it.executeQuery().toList { statusEndring().toStatusEndring(arsaker = emptyList()) }.firstOrNull()
        }
    }

    override fun updateStatusEndringSykmeldtFnr(
        nyPersonident: PersonIdent,
        inactiveIdenter: List<PersonIdent>,
    ): Int {
        var updatedRows = 0
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_PERSONIDENT).use {
                inactiveIdenter.forEach { oldIdent ->
                    it.setString(1, nyPersonident.value)
                    it.setString(2, oldIdent.value)
                    updatedRows += it.executeUpdate()
                }
            }
            connection.commit()
        }
        return updatedRows
    }

    private fun Connection.getArsaker(statusEndringId: Int): List<Arsak> =
        prepareStatement(GET_ARSAKER_BY_STATUSENDRING_ID).use {
            it.setInt(1, statusEndringId)
            it.executeQuery().toList { toPArsak().toArsak() }
        }

    companion object {
        const val INSERT_ARSAK =
            """INSERT INTO ARSAK (
                id,
                uuid,
                status_endring_id,
                arsaktype,
                opprettet) VALUES (DEFAULT, ?, ?, ?, ?)
            """

        const val INSERT_STATUS_ENDRING =
            """INSERT INTO status_endring (
                id,
                uuid,
                sykmeldt_fnr,
                veileder_ident,
                status,
                virksomhet_nr,
                enhet_nr,
                opprettet) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?) RETURNING id
            """

        const val GET_STATUS_ENDRING_BY_PERSONIDENT =
            """
                SELECT *
                FROM status_endring
                WHERE sykmeldt_fnr = ?
                ORDER BY opprettet DESC
            """

        const val GET_ARSAKER_BY_STATUSENDRING_ID =
            """
                SELECT * 
                FROM arsak
                WHERE status_endring_id = ?
                ORDER BY id ASC
            """

        const val GET_STATUS_ENDRING_BY_UUID =
            """
                SELECT *
                FROM status_endring
                WHERE uuid = ?
            """

        const val UPDATE_PERSONIDENT =
            """
                UPDATE STATUS_ENDRING
                SET sykmeldt_fnr = ?
                WHERE sykmeldt_fnr = ?
            """
    }
}

internal fun ResultSet.toPArsak(): PArsak =
    PArsak(
        id = getInt("id"),
        uuid = getString("uuid"),
        statusEndringId = getInt("status_endring_id"),
        arsakType = getString("arsaktype"),
        opprettet = getTimestamp("opprettet").toInstant().atOffset(ZoneOffset.UTC)
    )

internal fun ResultSet.statusEndring(): PStatusEndring =
    PStatusEndring(
        id = getInt("id"),
        uuid = getString("uuid"),
        veilederIdent = VeilederIdent(getString("veileder_ident")),
        personIdent = PersonIdent(getString("sykmeldt_fnr")),
        status = Status.valueOf(getString("status")),
        virksomhetNr = getString("virksomhet_nr")?.let { VirksomhetNr(it) },
        opprettet = getTimestamp("opprettet").toInstant().atOffset(ZoneOffset.UTC),
        enhetNr = getString("enhet_nr")?.let { EnhetNr(it) }
    )
