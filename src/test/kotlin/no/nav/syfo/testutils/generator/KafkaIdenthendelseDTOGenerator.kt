package no.nav.syfo.testutils.generator

import no.nav.syfo.identhendelse.kafka.IdentType
import no.nav.syfo.identhendelse.kafka.Identifikator
import no.nav.syfo.identhendelse.kafka.KafkaIdenthendelseDTO
import no.nav.syfo.pengestopp.SykmeldtFnr
import no.nav.syfo.testutils.UserConstants

fun generateKafkaIdenthendelseDTO(
    sykmeldtFnr: SykmeldtFnr = UserConstants.ARBEIDSTAKER_PERSONIDENT,
    hasOldPersonident: Boolean,
): KafkaIdenthendelseDTO {
    val identifikatorer = mutableListOf(
        Identifikator(
            idnummer = sykmeldtFnr.value,
            type = IdentType.FOLKEREGISTERIDENT,
            gjeldende = true,
        ),
        Identifikator(
            idnummer = "10${sykmeldtFnr.value}",
            type = IdentType.AKTORID,
            gjeldende = true
        ),
    )
    if (hasOldPersonident) {
        identifikatorer.addAll(
            listOf(
                Identifikator(
                    idnummer = UserConstants.ARBEIDSTAKER_PERSONIDENT_2.value,
                    type = IdentType.FOLKEREGISTERIDENT,
                    gjeldende = false,
                ),
                Identifikator(
                    idnummer = "9${UserConstants.ARBEIDSTAKER_PERSONIDENT_2.value.drop(1)}",
                    type = IdentType.FOLKEREGISTERIDENT,
                    gjeldende = false,
                ),
            )
        )
    }
    return KafkaIdenthendelseDTO(identifikatorer)
}
