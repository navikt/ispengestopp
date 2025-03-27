package no.nav.syfo.application

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.StatusEndring
import no.nav.syfo.pengestopp.SykepengestoppArsak
import java.time.LocalDate

class PengestoppService(
    private val pengestoppRepository: IPengestoppRepository,
    private val statusEndringProducer: IStatusEndringProducer,
) {
    fun createStatusendringer(statusEndringer: List<StatusEndring>) {
        pengestoppRepository.createStatusEndringer(statusEndringer)
        statusEndringer.forEach { statusEndringProducer.send(it) }
    }

    fun getOldStatusendringer(personIdent: PersonIdent): List<StatusEndring> {
        return pengestoppRepository.getStatusEndringer(personIdent)
            .filter { isOldStatusendring(it) }
            .filter { atLeastOneValidArsak(it) }
            .map { removeDeprecatedArsak(it) }
    }

    private fun isOldStatusendring(statusEndring: StatusEndring): Boolean {
        return when (statusEndring.arsakList.first().type) {
            SykepengestoppArsak.MANGLENDE_MEDVIRKING -> statusEndring.opprettet.toLocalDate().isBefore(
                CUTOFF_DATE_MANGLENDE_MEDVIRKNING
            )
            SykepengestoppArsak.MEDISINSK_VILKAR -> statusEndring.opprettet.toLocalDate().isBefore(
                CUTOFF_DATE_ARBEIDSUFORHET
            )
            SykepengestoppArsak.AKTIVITETSKRAV -> statusEndring.opprettet.toLocalDate().isBefore(
                CUTOFF_DATE_AKTIVITETSKRAV
            )
            else -> true
        }
    }

    private fun atLeastOneValidArsak(statusEndring: StatusEndring): Boolean {
        return !statusEndring.arsakList.all { it.type.isDeprecated } || statusEndring.arsakList.isNotEmpty()
    }

    private fun removeDeprecatedArsak(statusEndring: StatusEndring): StatusEndring {
        return statusEndring.copy(
            arsakList = statusEndring.arsakList.filter {
                !it.type.isDeprecated
            }
        )
    }

    companion object {
        val CUTOFF_DATE_MANGLENDE_MEDVIRKNING: LocalDate = LocalDate.of(2025, 3, 10)
        val CUTOFF_DATE_ARBEIDSUFORHET: LocalDate = LocalDate.of(2025, 3, 11)
        val CUTOFF_DATE_AKTIVITETSKRAV: LocalDate = LocalDate.of(2025, 3, 12)
    }
}
