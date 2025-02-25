package no.nav.syfo.infrastructure.database

import no.nav.syfo.pengestopp.*
import no.nav.syfo.testutils.TestDB
import no.nav.syfo.testutils.UserConstants
import no.nav.syfo.testutils.dropData
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.util.UUID

class PengestoppRepositorySpek : Spek({
    describe(PengestoppRepository::class.java.simpleName) {

        val database = TestDB()
        val repository = PengestoppRepository(database = database)

        afterEachTest {
            database.connection.use { it.dropData() }
        }

        it("creates StatusEndring without virksomhetNr and enhetNr") {
            val statusEndring = StatusEndring(
                uuid = UUID.randomUUID().toString(),
                veilederIdent = VeilederIdent("Z999999"),
                sykmeldtFnr = UserConstants.SYKMELDT_PERSONIDENT,
                status = Status.STOPP_AUTOMATIKK,
                arsakList = listOf(Arsak(type = SykepengestoppArsak.MANGLENDE_MEDVIRKING)),
                virksomhetNr = null,
                opprettet = OffsetDateTime.now(),
                enhetNr = null,
            )

            repository.createStatusEndring(statusEndring)

            val storedStatusendring = repository.getStatusEndringer(UserConstants.SYKMELDT_PERSONIDENT).first()

            storedStatusendring.uuid shouldBeEqualTo statusEndring.uuid
            storedStatusendring.sykmeldtFnr shouldBeEqualTo UserConstants.SYKMELDT_PERSONIDENT
            storedStatusendring.virksomhetNr.shouldBeNull()
            storedStatusendring.enhetNr.shouldBeNull()
        }
    }
})
