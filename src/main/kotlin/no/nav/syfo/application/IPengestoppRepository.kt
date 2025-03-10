package no.nav.syfo.application

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.StatusEndring
import java.util.UUID

interface IPengestoppRepository {
    fun createStatusEndring(statusEndring: StatusEndring)
    fun createStatusEndringer(statusEndringer: List<StatusEndring>)
    fun getStatusEndringer(personIdent: PersonIdent): List<StatusEndring>
    fun getStatusEndring(uuid: UUID): StatusEndring?
    fun updateStatusEndringSykmeldtFnr(nyPersonident: PersonIdent, inactiveIdenter: List<PersonIdent>): Int
}
