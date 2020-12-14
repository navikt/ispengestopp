package no.nav.syfo.api.testutils

import no.nav.syfo.EnhetNr
import no.nav.syfo.Status
import no.nav.syfo.SykmeldtFnr
import no.nav.syfo.VeilederIdent
import no.nav.syfo.VirksomhetNr

data class DBStatusChangeTest(
    val uuid: String,
    val sykmeldtFnr: SykmeldtFnr,
    val veilederIdent: VeilederIdent,
    val status: Status,
    val virksomhetNr: VirksomhetNr,
    val enhetNr: EnhetNr
)
