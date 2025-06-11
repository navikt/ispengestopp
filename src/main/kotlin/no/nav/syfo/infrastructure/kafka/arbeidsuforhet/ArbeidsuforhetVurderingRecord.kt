package no.nav.syfo.infrastructure.kafka.arbeidsuforhet

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class ArbeidsuforhetVurderingRecord(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: String,
    val veilederident: String,
    val type: VurderingType,
    val arsak: VurderingArsak?,
    val begrunnelse: String,
    val gjelderFom: LocalDate?,
    val isFinal: Boolean,
)

enum class VurderingType(val isFinal: Boolean) {
    FORHANDSVARSEL(false),
    OPPFYLT(true),
    OPPFYLT_UTEN_FORHANDSVARSEL(true),
    AVSLAG(true),
    AVSLAG_UTEN_FORHANDSVARSEL(true),
    IKKE_AKTUELL(true);
}

enum class VurderingArsak {
    FRISKMELDT,
    FRISKMELDING_TIL_ARBEIDSFORMIDLING,
    SYKEPENGER_IKKE_UTBETALT,
    NAY_BER_OM_NY_VURDERING,
}
