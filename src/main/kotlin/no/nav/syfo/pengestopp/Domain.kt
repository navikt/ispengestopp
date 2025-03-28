package no.nav.syfo.pengestopp

import no.nav.syfo.domain.PersonIdent
import java.time.LocalDate
import java.time.OffsetDateTime

data class EnhetNr(val value: String) {
    init {
        nrValidator(value)
    }
}

fun nrValidator(value: String) {
    if (value.isBlank()) {
        throw IllegalArgumentException("Nr cannot be empty")
    }
    try {
        Integer.parseInt(value)
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Nr must be numerical")
    }
}

data class StatusEndring(
    val uuid: String,
    val veilederIdent: VeilederIdent,
    val sykmeldtFnr: PersonIdent,
    val status: Status,
    val arsakList: List<Arsak>,
    val virksomhetNr: VirksomhetNr?,
    val opprettet: OffsetDateTime,
    val enhetNr: EnhetNr?
) {
    private val CUTOFF_DATE_MANGLENDE_MEDVIRKNING = LocalDate.of(2025, 3, 10)
    private val CUTOFF_DATE_ARBEIDSUFORHET = LocalDate.of(2025, 3, 11)
    private val CUTOFF_DATE_AKTIVITETSKRAV = LocalDate.of(2025, 3, 12)

    val isManuell: Boolean = arsakList.isEmpty() || arsakList.size > 1 || isBeforeCutoffDate()

    private fun isBeforeCutoffDate(): Boolean {
        val arsak = arsakList.firstOrNull()
        val opprettetDate = opprettet.toLocalDate()
        return when (arsak?.type) {
            SykepengestoppArsak.MANGLENDE_MEDVIRKING -> opprettetDate.isBefore(CUTOFF_DATE_MANGLENDE_MEDVIRKNING)
            SykepengestoppArsak.MEDISINSK_VILKAR -> opprettetDate.isBefore(CUTOFF_DATE_ARBEIDSUFORHET)
            SykepengestoppArsak.AKTIVITETSKRAV -> opprettetDate.isBefore(CUTOFF_DATE_AKTIVITETSKRAV)
            else -> true
        }
    }
}

enum class SykepengestoppArsak {
    BESTRIDELSE_SYKMELDING,
    MEDISINSK_VILKAR,
    AKTIVITETSKRAV,
    TILBAKEDATERT_SYKMELDING,
    MANGLENDE_MEDVIRKING,
}

data class Arsak(
    val type: SykepengestoppArsak
)

data class VeilederIdent(val value: String)
data class VirksomhetNr(val value: String) {
    init {
        nrValidator(value)
    }
}

enum class Status { NORMAL, STOPP_AUTOMATIKK }
