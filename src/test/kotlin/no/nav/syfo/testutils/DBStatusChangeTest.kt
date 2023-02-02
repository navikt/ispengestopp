package no.nav.syfo.testutils

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.*
import java.time.OffsetDateTime

data class DBStatusChangeTest(
    val uuid: String,
    val personIdent: PersonIdent,
    val veilederIdent: VeilederIdent,
    val status: Status,
    val arsakList: List<Arsak>?,
    val virksomhetNr: VirksomhetNr,
    val enhetNr: EnhetNr,
    val opprettet: OffsetDateTime,
)
