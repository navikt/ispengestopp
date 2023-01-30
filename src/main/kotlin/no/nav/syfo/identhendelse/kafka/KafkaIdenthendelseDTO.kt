package no.nav.syfo.identhendelse.kafka

import no.nav.syfo.pengestopp.SykmeldtFnr

// Basert på https://github.com/navikt/pdl/blob/master/libs/contract-pdl-avro/src/main/avro/no/nav/person/pdl/aktor/AktorV2.avdl

data class KafkaIdenthendelseDTO(
    val identifikatorer: List<Identifikator>,
) {
    val folkeregisterIdenter: List<Identifikator> = identifikatorer.filter { it.type == IdentType.FOLKEREGISTERIDENT }

    fun getActivePersonident(): SykmeldtFnr? = folkeregisterIdenter
        .find { it.gjeldende }
        ?.let { SykmeldtFnr(it.idnummer) }

    fun getInactivePersonidenter(): List<SykmeldtFnr> = folkeregisterIdenter
        .filter { !it.gjeldende }
        .map { SykmeldtFnr(it.idnummer) }
}

data class Identifikator(
    val idnummer: String,
    val type: IdentType,
    val gjeldende: Boolean,
)

enum class IdentType {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}
