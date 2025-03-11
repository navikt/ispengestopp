package no.nav.syfo.infrastructure.kafka.aktivitetskrav

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class AktivitetskravVurderingRecord(
    val uuid: UUID,
    val personIdent: String,
    val createdAt: OffsetDateTime,
    val status: String,
    val isFinal: Boolean,
    val beskrivelse: String?,
    val arsaker: List<String>,
    val stoppunktAt: LocalDate,
    val updatedBy: String?,
    val sisteVurderingUuid: UUID?,
    val sistVurdert: OffsetDateTime?,
    val frist: LocalDate?,
    val previousAktivitetskravUuid: UUID?,
)

enum class AktivitetskravStatus(val isFinal: Boolean) {
    NY(false),
    NY_VURDERING(false),
    AVVENT(false),
    UNNTAK(true),
    OPPFYLT(true),
    AUTOMATISK_OPPFYLT(true),
    FORHANDSVARSEL(false),
    INNSTILLING_OM_STANS(true),
    IKKE_OPPFYLT(true),
    IKKE_AKTUELL(true),
    LUKKET(true),
}
