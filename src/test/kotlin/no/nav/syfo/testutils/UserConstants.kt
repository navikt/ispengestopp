package no.nav.syfo.testutils

import no.nav.syfo.domain.PersonIdent

object UserConstants {
    val SYKMELDT_PERSONIDENT = PersonIdent("12345678912")
    val SYKMELDT_PERSONIDENT_IKKE_TILGANG = PersonIdent("66666666666")
    val SYKMELDT_PERSONIDENT_2 = PersonIdent(SYKMELDT_PERSONIDENT.value.replace("2", "8"))
    val SYKMELDT_PERSONIDENT_3 = PersonIdent("12345678913")
}
