package no.nav.syfo.testutils.generator

import no.nav.syfo.infrastructure.kafka.manglendemedvirkning.ManglendeMedvirkningVurderingRecord
import no.nav.syfo.infrastructure.kafka.manglendemedvirkning.VurderingType
import no.nav.syfo.infrastructure.kafka.manglendemedvirkning.VurderingTypeDTO
import no.nav.syfo.testutils.UserConstants
import java.time.OffsetDateTime
import java.util.*

fun genereateManglendeMedvirkningVurdering(type: VurderingType): ManglendeMedvirkningVurderingRecord =
    ManglendeMedvirkningVurderingRecord(
        uuid = UUID.randomUUID(),
        personident = UserConstants.SYKMELDT_PERSONIDENT.value,
        veilederident = "Z999999",
        createdAt = OffsetDateTime.now(),
        vurderingType = VurderingTypeDTO(type)
    )
