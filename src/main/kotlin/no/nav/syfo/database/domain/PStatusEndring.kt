package no.nav.syfo.database.domain

import no.nav.syfo.*
import java.time.OffsetDateTime

data class PStatusEndring(
    val id: Int,
    val uuid: String?,
    val sykmeldtFnr: SykmeldtFnr,
    val veilederIdent: VeilederIdent,
    val status: Status,
    val virksomhetNr: VirksomhetNr,
    val opprettet: OffsetDateTime,
    val enhetNr: EnhetNr
)

fun PStatusEndring.toStatusEndring(): StatusEndring =
    StatusEndring(
        uuid = this.uuid,
        sykmeldtFnr = this.sykmeldtFnr,
        veilederIdent = this.veilederIdent,
        status = this.status,
        virksomhetNr = this.virksomhetNr,
        opprettet = this.opprettet,
        enhetNr = this.enhetNr
    )
