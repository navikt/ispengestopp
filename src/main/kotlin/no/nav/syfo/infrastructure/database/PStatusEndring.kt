package no.nav.syfo.infrastructure.database

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.*
import java.time.OffsetDateTime

data class PStatusEndring(
    val id: Int,
    val uuid: String,
    val personIdent: PersonIdent,
    val veilederIdent: VeilederIdent,
    val status: Status,
    val virksomhetNr: VirksomhetNr,
    val opprettet: OffsetDateTime,
    val enhetNr: EnhetNr
)

fun PStatusEndring.toStatusEndring(arsaker: List<Arsak>): StatusEndring =
    StatusEndring(
        uuid = this.uuid,
        sykmeldtFnr = this.personIdent,
        veilederIdent = this.veilederIdent,
        status = this.status,
        arsakList = arsaker,
        virksomhetNr = this.virksomhetNr,
        opprettet = this.opprettet,
        enhetNr = this.enhetNr
    )
