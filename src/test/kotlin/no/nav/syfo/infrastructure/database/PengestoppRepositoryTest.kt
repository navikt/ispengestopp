package no.nav.syfo.infrastructure.database

import no.nav.syfo.pengestopp.*
import no.nav.syfo.testutils.TestDB
import no.nav.syfo.testutils.UserConstants
import no.nav.syfo.testutils.dropData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class PengestoppRepositoryTest {

    private val database = TestDB()
    private val repository = PengestoppRepository(database = database)

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Test
    fun `creates StatusEndring without virksomhetNr and enhetNr`() {
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

        assertEquals(statusEndring.uuid, storedStatusendring.uuid)
        assertEquals(UserConstants.SYKMELDT_PERSONIDENT, storedStatusendring.sykmeldtFnr)
        assertNull(storedStatusendring.virksomhetNr)
        assertNull(storedStatusendring.enhetNr)
    }
}
