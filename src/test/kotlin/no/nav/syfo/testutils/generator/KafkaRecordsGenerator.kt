package no.nav.syfo.testutils.generator

import no.nav.syfo.infrastructure.kafka.arbeidsuforhet.ArbeidsuforhetVurderingRecord
import no.nav.syfo.infrastructure.kafka.manglendemedvirkning.ManglendeMedvirkningVurderingRecord
import no.nav.syfo.infrastructure.kafka.manglendemedvirkning.VurderingType
import no.nav.syfo.infrastructure.kafka.manglendemedvirkning.VurderingTypeDTO
import no.nav.syfo.testutils.UserConstants
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import no.nav.syfo.infrastructure.kafka.arbeidsuforhet.VurderingType as ArbeidsuforhetVurderingType

fun genereateManglendeMedvirkningVurdering(type: VurderingType): ManglendeMedvirkningVurderingRecord =
    ManglendeMedvirkningVurderingRecord(
        uuid = UUID.randomUUID(),
        personident = UserConstants.SYKMELDT_PERSONIDENT.value,
        veilederident = "Z999999",
        createdAt = OffsetDateTime.now(),
        vurderingType = VurderingTypeDTO(type)
    )

fun generateArbeidsuforhetVurdering(type: ArbeidsuforhetVurderingType) =
    ArbeidsuforhetVurderingRecord(
        uuid = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        personident = UserConstants.SYKMELDT_PERSONIDENT.value,
        veilederident = "Z999999",
        type = type,
        arsak = null,
        begrunnelse = "tekst",
        gjelderFom = LocalDate.now(),
        isFinal = true,
    )
