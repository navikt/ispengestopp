package no.nav.syfo.identhendelse.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdent

const val queryUpdateSykmeldtFnr =
    """
        UPDATE STATUS_ENDRING
        SET sykmeldt_fnr = ?
        WHERE sykmeldt_fnr = ?
    """

fun DatabaseInterface.updateStatusEndringSykmeldtFnr(
    nyPersonident: PersonIdent,
    inactiveIdenter: List<PersonIdent>,
): Int {
    var updatedRows = 0
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdateSykmeldtFnr).use {
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
