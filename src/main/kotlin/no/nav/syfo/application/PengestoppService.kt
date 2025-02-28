package no.nav.syfo.application

import no.nav.syfo.pengestopp.StatusEndring

class PengestoppService(
    private val pengestoppRepository: IPengestoppRepository,
    private val statusEndringProducer: IStatusEndringProducer,
) {
    fun createStatusendringer(statusEndringer: List<StatusEndring>) {
        pengestoppRepository.createStatusEndringer(statusEndringer)
        statusEndringer.forEach { statusEndringProducer.send(it) }
    }
}
