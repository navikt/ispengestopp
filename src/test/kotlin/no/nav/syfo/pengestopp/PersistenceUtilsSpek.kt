package no.nav.syfo.pengestopp

import io.mockk.*
import no.nav.common.KafkaEnvironment
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.objectMapper
import no.nav.syfo.pengestopp.database.getActiveFlags
import no.nav.syfo.testutils.*
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*
import java.util.*

object PersistenceUtilsSpek : Spek({
    val embeddedKafkaEnvironment = KafkaEnvironment(
        autoStart = false,
        topicNames = listOf("teamsykefravr.apen-isyfo-stoppautomatikk")
    )

    val sykmeldtPersonIdent = UserConstants.SYKMELDT_PERSONIDENT
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
    val enhetNr = EnhetNr("9999")

    val env = testEnvironment(
        kafkaBrokersURL = embeddedKafkaEnvironment.brokersURL,
    )

    val partition = 0
    val stoppAutomatikkTopicPartition = TopicPartition(env.stoppAutomatikkAivenTopic, partition)

    val arsakList = listOf(
        Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING),
        Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV)
    )

    val incomingStatusEndring = StatusEndring(
        UUID.randomUUID().toString(),
        veilederIdent,
        sykmeldtPersonIdent,
        Status.STOPP_AUTOMATIKK,
        arsakList,
        primaryJob,
        Instant.now().atZone(ZoneOffset.UTC).toOffsetDateTime(),
        enhetNr
    )
    val hendelse = objectMapper.writeValueAsString(incomingStatusEndring)
    val hendelseRecord = ConsumerRecord(env.stoppAutomatikkAivenTopic, partition, 1, "something", hendelse)

    fun verifyEmptyDB(database: DatabaseInterface) {
        val statusendringListe: List<StatusEndring> = database.getActiveFlags(personIdent = sykmeldtPersonIdent)
        statusendringListe.size shouldBeEqualTo 0
    }

    describe("PollAndPersist") {
        val database = TestDB()

        afterGroup {
            database.stop()
            unmockkAll()
        }

        afterEachTest {
            database.connection.dropData()
            unmockkAll()
        }

        beforeEachTest { verifyEmptyDB(database) }

        val mockConsumer = mockk<KafkaConsumer<String, String>>()
        every { mockConsumer.poll(Duration.ofMillis(env.pollTimeOutMs)) } returns ConsumerRecords(
            mapOf(stoppAutomatikkTopicPartition to listOf(hendelseRecord))
        )
        every { mockConsumer.commitSync() } returns Unit

        it("Store in database after reading from kafka") {
            pollAndPersist(mockConsumer, database, env)

            val statusendringListe: List<StatusEndring> = database.getActiveFlags(personIdent = sykmeldtPersonIdent)
            statusendringListe.size shouldBeEqualTo 1

            val statusEndring = statusendringListe[0]
            statusEndring.sykmeldtFnr shouldBeEqualTo sykmeldtPersonIdent
            statusEndring.veilederIdent shouldBeEqualTo veilederIdent
            statusEndring.virksomhetNr shouldBeEqualTo primaryJob
            statusEndring.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
            statusEndring.opprettet.dayOfMonth shouldBeEqualTo
                Instant.now().atZone(ZoneOffset.UTC).toOffsetDateTime().dayOfMonth
            statusEndring.enhetNr shouldBeEqualTo enhetNr
        }

        it("Do not store in database after reading already persisted record") {
            pollAndPersist(mockConsumer, database, env)

            pollAndPersist(mockConsumer, database, env)

            val statusendringListe: List<StatusEndring> = database.getActiveFlags(personIdent = sykmeldtPersonIdent)
            statusendringListe.size shouldBeEqualTo 1

            val statusEndring = statusendringListe.first()
            statusEndring.arsakList shouldBeEqualTo arsakList
            statusEndring.sykmeldtFnr shouldBeEqualTo sykmeldtPersonIdent
            statusEndring.veilederIdent shouldBeEqualTo veilederIdent
            statusEndring.virksomhetNr shouldBeEqualTo primaryJob
            statusEndring.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
            statusEndring.opprettet.dayOfMonth shouldBeEqualTo
                Instant.now().atZone(ZoneOffset.UTC).toOffsetDateTime().dayOfMonth
            statusEndring.enhetNr shouldBeEqualTo enhetNr

            COUNT_ENDRE_PERSON_STATUS_DB_ALREADY_STORED.count() shouldBeEqualTo 1.0
        }
    }
})
