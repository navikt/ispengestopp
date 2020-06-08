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

fun DatabaseInterface.createSmAutomatiskBehandling(data: String, sykmeldingId: String) {
    connection.use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO smAutomatiskBehandling(
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

fun DatabaseInterface.createSmHist(data: String, sykmeldingId: String) {
    connection.use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO smHist(
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

fun DatabaseInterface.createSmBehandlingsutfall(data: String, sykmeldingId: String) {
    connection.use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO smBehandlingsutfall(
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

fun DatabaseInterface.createSmBehandlingsutfallHist(data: String, sykmeldingId: String) {
    connection.use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO smBehandlingsutfallHist(
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

fun DatabaseInterface.createSmSykmeldingstatus(data: String, sykmeldingId: String) {
    connection.use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO smSykmeldingstatus(
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

fun DatabaseInterface.createSmSykmeldingstatusHist(data: String, sykmeldingId: String) {
    connection.use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO smSykmeldingstatusHist(
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
