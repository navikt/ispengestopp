package no.nav.syfo.infrastructure.kafka.manglendemedvirkning

import java.time.OffsetDateTime
import java.util.*

data class ManglendeMedvirkningVurderingRecord(
    val uuid: UUID,
    val personident: String,
    val veilederident: String,
    val createdAt: OffsetDateTime,
    val vurderingType: VurderingTypeDTO,
)

data class VurderingTypeDTO(
    val value: VurderingType,
)

enum class VurderingType {
    FORHANDSVARSEL, OPPFYLT, STANS, IKKE_AKTUELL, UNNTAK
}
