package no.nav.syfo.pengestopp.database.domain

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

fun PStatusEndring.toStatusEndring(): StatusEndring =
    StatusEndring(
        uuid = this.uuid,
        sykmeldtFnr = this.personIdent,
        veilederIdent = this.veilederIdent,
        status = this.status,
        arsakList = null,
        virksomhetNr = this.virksomhetNr,
        opprettet = this.opprettet,
        enhetNr = this.enhetNr
    )
