package no.nav.syfo.pengestopp

import no.nav.syfo.domain.PersonIdent
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
    val enhetNr: EnhetNr? // For å holde oversikt over hvem som bruker tjenesten
)

enum class SykepengestoppArsak(val isDeprecated: Boolean = false) {
    BESTRIDELSE_SYKMELDING(true),
    MEDISINSK_VILKAR,
    AKTIVITETSKRAV,
    TILBAKEDATERT_SYKMELDING(true),
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
