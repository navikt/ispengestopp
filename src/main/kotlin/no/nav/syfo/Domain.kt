package no.nav.syfo

import java.time.OffsetDateTime

data class EnhetNr(val value: String)

data class KFlaggperson84Hendelse(
    val veilederIdent: VeilederIdent,
    val sykmeldtFnr: SykmeldtFnr,
    val status: Status,
    val virksomhetNr: VirksomhetNr,
    val opprettet: OffsetDateTime,
    val enhetNr: EnhetNr
)

data class StoppAutomatikk(
    val sykmeldtFnr: SykmeldtFnr,
    val virksomhetNr: List<VirksomhetNr>,
    val veilederIdent: VeilederIdent,
    val enhetNr: EnhetNr
)

data class DBStatusChangeTest(
    val uuid: String,
    val sykmeldtFnr: SykmeldtFnr,
    val veilederIdent: VeilederIdent,
    val status: Status,
    val virksomhetNr: VirksomhetNr,
    val enhetNr: EnhetNr,
    val opprettet: OffsetDateTime
)

data class SykmeldtFnr(val value: String)
data class Tilgang(val harTilgang: Boolean, val begrunnelse: String? = null)
data class VeilederIdent(val value: String)
data class VirksomhetNr(val value: String)

enum class Status { NORMAL, STOPP_AUTOMATIKK }
