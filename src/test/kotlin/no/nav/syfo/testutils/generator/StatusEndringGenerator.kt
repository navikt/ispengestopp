package no.nav.syfo.testutils.generator

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.*
import no.nav.syfo.testutils.DBStatusChangeTest
import no.nav.syfo.testutils.UserConstants
import java.time.OffsetDateTime

fun generateStatusEndringer(
    personIdent: PersonIdent = UserConstants.SYKMELDT_PERSONIDENT,
    veilederIdent: VeilederIdent = VeilederIdent("Z999999"),
    primaryJob: VirksomhetNr = VirksomhetNr("888"),
    secondaryJob: VirksomhetNr = VirksomhetNr("999"),
    enhetNr: EnhetNr = EnhetNr("9999"),
    opprettet: OffsetDateTime = OffsetDateTime.now(),
    personIdentFiller: PersonIdent = PersonIdent("12312312344"),
    arsakList: List<Arsak> = listOf(
        Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING),
        Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV)
    )
): List<DBStatusChangeTest> {
    return listOf(
        DBStatusChangeTest(
            "1",
            personIdent,
            veilederIdent,
            Status.STOPP_AUTOMATIKK,
            arsakList,
            primaryJob,
            enhetNr,
            opprettet,
        ),
        DBStatusChangeTest(
            "2",
            personIdent,
            veilederIdent,
            Status.STOPP_AUTOMATIKK,
            arsakList,
            primaryJob,
            enhetNr,
            opprettet,
        ),
        DBStatusChangeTest(
            "3",
            personIdent,
            veilederIdent,
            Status.STOPP_AUTOMATIKK,
            arsakList,
            secondaryJob,
            enhetNr,
            opprettet,
        ),
        DBStatusChangeTest(
            "4",
            personIdentFiller,
            veilederIdent,
            Status.STOPP_AUTOMATIKK,
            arsakList,
            primaryJob,
            enhetNr,
            opprettet,
        )
    )
}
