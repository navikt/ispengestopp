package no.nav.syfo.testutils.generator

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.pengestopp.*
import no.nav.syfo.testutils.UserConstants
import java.time.OffsetDateTime
import java.util.UUID

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
): List<StatusEndring> {
    return listOf(
        StatusEndring(
            UUID.randomUUID().toString(),
            veilederIdent,
            personIdent,
            Status.STOPP_AUTOMATIKK,
            arsakList,
            primaryJob,
            opprettet,
            enhetNr,
        ),
        StatusEndring(
            UUID.randomUUID().toString(),
            veilederIdent,
            personIdent,
            Status.STOPP_AUTOMATIKK,
            arsakList,
            primaryJob,
            opprettet,
            enhetNr,
        ),
        StatusEndring(
            UUID.randomUUID().toString(),
            veilederIdent,
            personIdent,
            Status.STOPP_AUTOMATIKK,
            arsakList,
            secondaryJob,
            opprettet,
            enhetNr,
        ),
        StatusEndring(
            UUID.randomUUID().toString(),
            veilederIdent,
            personIdentFiller,
            Status.STOPP_AUTOMATIKK,
            arsakList,
            primaryJob,
            opprettet,
            enhetNr,
        )
    )
}
