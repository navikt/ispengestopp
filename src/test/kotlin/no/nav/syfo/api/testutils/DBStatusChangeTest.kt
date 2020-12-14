package no.nav.syfo.api.testutils

import no.nav.syfo.*

data class DBStatusChangeTest(
    val uuid: String,
    val sykmeldtFnr: SykmeldtFnr,
    val veilederIdent: VeilederIdent,
    val status: Status,
    val arsakList: List<Arsak>?,
    val virksomhetNr: VirksomhetNr,
    val enhetNr: EnhetNr
)
