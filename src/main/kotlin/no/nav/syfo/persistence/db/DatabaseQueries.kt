package no.nav.syfo.persistence.db

import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.util.toPGObject
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun DatabaseInterface.createSmManuellBehandling(data: String, sykmeldingId: String) {
    connection.use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO smManuellBehandling(
                sykmelding_id,
                created,
                data
                )
            VALUES  (?, ?, ?)
            """
        ).use {
            it.setString(1, sykmeldingId)
            it.setTimestamp(2, Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()))
            it.setObject(3, data.toPGObject())
            it.executeUpdate()
        }

        connection.commit()
    }
}
