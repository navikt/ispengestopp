package no.nav.syfo

import no.nav.syfo.util.nrValidator
import java.time.OffsetDateTime

data class EnhetNr(val value: String) {
    init {
        nrValidator(value)
    }
}

data class StatusEndring(
    val uuid: String?,
    val veilederIdent: VeilederIdent,
    val sykmeldtFnr: SykmeldtFnr,
    val status: Status,
    val virksomhetNr: VirksomhetNr,
    val opprettet: OffsetDateTime,
    val enhetNr: EnhetNr // For å holde oversikt over hvem som bruker tjenesten
)

data class StoppAutomatikk(
    val sykmeldtFnr: SykmeldtFnr,
    val virksomhetNr: List<VirksomhetNr>,
    val enhetNr: EnhetNr
)

data class SykmeldtFnr(val value: String)
data class Tilgang(val harTilgang: Boolean, val begrunnelse: String? = null)
data class VeilederIdent(val value: String)
data class VirksomhetNr(val value: String) {
    init {
        nrValidator(value)
    }
}

enum class Status { NORMAL, STOPP_AUTOMATIKK }
