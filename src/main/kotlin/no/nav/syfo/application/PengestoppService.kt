package no.nav.syfo.application

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.StatusEndring

class PengestoppService(
    private val pengestoppRepository: IPengestoppRepository,
    private val statusEndringProducer: IStatusEndringProducer,
) {
    fun createStatusendringer(statusEndringer: List<StatusEndring>) {
        pengestoppRepository.createStatusEndringer(statusEndringer)
        statusEndringer.forEach { statusEndringProducer.send(it) }
    }

    fun getManuelleStatusendringer(personIdent: PersonIdent): List<StatusEndring> {
        return pengestoppRepository
            .getStatusEndringer(personIdent)
            .filter { it.isManuell }
            .filter { atLeastOneValidArsak(it) }
            .map { removeDeprecatedArsak(it) }
    }

    private fun atLeastOneValidArsak(statusEndring: StatusEndring): Boolean {
        return !statusEndring.arsakList.all { it.type.isDeprecated } || statusEndring.arsakList.isNotEmpty()
    }

    private fun removeDeprecatedArsak(statusEndring: StatusEndring): StatusEndring {
        return statusEndring.copy(
            arsakList = statusEndring.arsakList.filter { !it.type.isDeprecated }
        )
    }
}
