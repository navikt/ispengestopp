package no.nav.syfo.testutils

import no.nav.syfo.pengestopp.*
import java.time.OffsetDateTime

data class DBStatusChangeTest(
    val uuid: String,
    val sykmeldtFnr: SykmeldtFnr,
    val veilederIdent: VeilederIdent,
    val status: Status,
    val arsakList: List<Arsak>?,
    val virksomhetNr: VirksomhetNr,
    val enhetNr: EnhetNr,
    val opprettet: OffsetDateTime,
)
