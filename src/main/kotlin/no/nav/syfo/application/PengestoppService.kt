package no.nav.syfo.application

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.log
import no.nav.syfo.pengestopp.COUNT_ENDRE_PERSON_STATUS_DB_ALREADY_STORED
import no.nav.syfo.pengestopp.StatusEndring
import java.util.UUID

class PengestoppService(
    private val pengestoppRepository: IPengestoppRepository,
    private val statusEndringProducer: IStatusEndringProducer,
) {
    fun createStatusendringer(statusEndringer: List<StatusEndring>) {
        val nyeStatusendringer = statusEndringer.filter { statusEndring ->
            val alleredeLagret = pengestoppRepository.getStatusEndring(uuid = UUID.fromString(statusEndring.uuid)) != null
            if (alleredeLagret) {
                log.warn("StatusEndring with uuid=${statusEndring.uuid} is already stored and is skipped")
                COUNT_ENDRE_PERSON_STATUS_DB_ALREADY_STORED.increment()
            }
            !alleredeLagret
        }
        if (nyeStatusendringer.isNotEmpty()) {
            pengestoppRepository.createStatusEndringer(nyeStatusendringer)
            nyeStatusendringer.forEach { statusEndringProducer.send(it) }
        }
    }

    fun getManuelleStatusendringer(personIdent: PersonIdent): List<StatusEndring> {
        return pengestoppRepository
            .getStatusEndringer(personIdent)
            .filter { it.isManuell }
            .filter { atLeastOneValidArsak(it) }
            .map { removeDeprecatedArsak(it) }
    }

    private fun atLeastOneValidArsak(statusEndring: StatusEndring): Boolean {
        return !statusEndring.arsakList.all { it.type.isDeprecated } || statusEndring.arsakList.isEmpty()
    }

    private fun removeDeprecatedArsak(statusEndring: StatusEndring): StatusEndring {
        return statusEndring.copy(
            arsakList = statusEndring.arsakList.filter { !it.type.isDeprecated }
        )
    }
}
