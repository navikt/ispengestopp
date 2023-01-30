package no.nav.syfo.testutils

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.SykmeldtFnr

object UserConstants {
    val SYKMELDT_FNR = SykmeldtFnr("123456")
    val SYKMELDT_FNR_IKKE_TILGANG = SykmeldtFnr("666")
    val ARBEIDSTAKER_PERSONIDENT = PersonIdent("12345678912")
    val ARBEIDSTAKER_PERSONIDENT_2 = PersonIdent(ARBEIDSTAKER_PERSONIDENT.value.replace("2", "8"))
    val ARBEIDSTAKER_PERSONIDENT_3 = PersonIdent("12345678913")
}
