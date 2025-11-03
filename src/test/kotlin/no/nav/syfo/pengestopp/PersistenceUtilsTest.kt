package no.nav.syfo.pengestopp

import io.mockk.*
import no.nav.syfo.application.IPengestoppRepository
import no.nav.syfo.infrastructure.database.PengestoppRepository
import no.nav.syfo.objectMapper
import no.nav.syfo.testutils.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.*
import java.util.*

class PersistenceUtilsTest {
    private val sykmeldtPersonIdent = UserConstants.SYKMELDT_PERSONIDENT
    private val veilederIdent = VeilederIdent("Z999999")
    private val primaryJob = VirksomhetNr("888")
    private val enhetNr = EnhetNr("9999")

    private val env = testEnvironment()

    private val partition = 0
    private val stoppAutomatikkTopicPartition = TopicPartition(env.stoppAutomatikkAivenTopic, partition)

    private val arsakList = listOf(
        Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING),
        Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV)
    )

    private val incomingStatusEndring = StatusEndring(
        UUID.randomUUID().toString(),
        veilederIdent,
        sykmeldtPersonIdent,
        Status.STOPP_AUTOMATIKK,
        arsakList,
        primaryJob,
        Instant.now().atZone(ZoneOffset.UTC).toOffsetDateTime(),
        enhetNr
    )
    private val hendelse = objectMapper.writeValueAsString(incomingStatusEndring)
    private val hendelseRecord = ConsumerRecord(env.stoppAutomatikkAivenTopic, partition, 1, "something", hendelse)

    private val database = TestDB()
    private val repository = PengestoppRepository(database = database)
    private val mockConsumer = mockk<KafkaConsumer<String, String>>()

    private fun verifyEmptyDB(repository: IPengestoppRepository) {
        val statusendringListe: List<StatusEndring> = repository.getStatusEndringer(personIdent = sykmeldtPersonIdent)
        assertEquals(0, statusendringListe.size)
    }

    @AfterEach
    fun afterEach() {
        database.connection.dropData()
        unmockkAll()
    }

    @BeforeEach
    fun beforeEach() {
        verifyEmptyDB(repository)
        every { mockConsumer.poll(Duration.ofMillis(env.pollTimeOutMs)) } returns ConsumerRecords(
            mapOf(stoppAutomatikkTopicPartition to listOf(hendelseRecord))
        )
        every { mockConsumer.commitSync() } returns Unit
    }

    @Test
    fun `Store in database after reading from kafka`() {
        pollAndPersist(mockConsumer, repository, env)

        val statusendringListe: List<StatusEndring> = repository.getStatusEndringer(personIdent = sykmeldtPersonIdent)
        assertEquals(1, statusendringListe.size)

        val statusEndring = statusendringListe[0]
        assertEquals(sykmeldtPersonIdent, statusEndring.sykmeldtFnr)
        assertEquals(veilederIdent, statusEndring.veilederIdent)
        assertEquals(primaryJob, statusEndring.virksomhetNr)
        assertEquals(Status.STOPP_AUTOMATIKK, statusEndring.status)
        assertEquals(
            Instant.now().atZone(ZoneOffset.UTC).toOffsetDateTime().dayOfMonth,
            statusEndring.opprettet.dayOfMonth
        )
        assertEquals(enhetNr, statusEndring.enhetNr)
    }

    @Test
    fun `Do not store in database after reading already persisted record`() {
        pollAndPersist(mockConsumer, repository, env)

        pollAndPersist(mockConsumer, repository, env)

        val statusendringListe: List<StatusEndring> = repository.getStatusEndringer(personIdent = sykmeldtPersonIdent)
        assertEquals(1, statusendringListe.size)

        val statusEndring = statusendringListe.first()
        assertEquals(arsakList, statusEndring.arsakList)
        assertEquals(sykmeldtPersonIdent, statusEndring.sykmeldtFnr)
        assertEquals(veilederIdent, statusEndring.veilederIdent)
        assertEquals(primaryJob, statusEndring.virksomhetNr)
        assertEquals(Status.STOPP_AUTOMATIKK, statusEndring.status)
        assertEquals(
            Instant.now().atZone(ZoneOffset.UTC).toOffsetDateTime().dayOfMonth,
            statusEndring.opprettet.dayOfMonth
        )
        assertEquals(enhetNr, statusEndring.enhetNr)

        assertEquals(1.0, COUNT_ENDRE_PERSON_STATUS_DB_ALREADY_STORED.count())
    }
}
