package no.nav.syfo

import java.time.LocalDateTime

data class EnhetNr(val value: String)
data class StatusEndring(
    val veilederIdent: VeilederIdent,
    val sykmeldtFnr: SykmeldtFnr,
    val status: Status,
    val virksomhetNr: VirksomhetNr,
    val opprettet: LocalDateTime,
    val enhetNr: EnhetNr
)

data class StoppAutomatikk(
    val sykmeldtFnr: SykmeldtFnr,
    val virksomhetNr: List<VirksomhetNr>,
    val veilederIdent: VeilederIdent,
    val enhetNr: EnhetNr
)

data class SykmeldtFnr(val value: String)
data class Tilgang(val harTilgang: Boolean, val begrunnelse: String? = null)
data class VeilederIdent(val value: String)
data class VirksomhetNr(val value: String)

enum class Status { NORMAL, STOPP_AUTOMATIKK }
