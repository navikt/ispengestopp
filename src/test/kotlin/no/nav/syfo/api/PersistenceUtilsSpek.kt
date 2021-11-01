package no.nav.syfo.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import no.nav.common.KafkaEnvironment
import no.nav.syfo.*
import no.nav.syfo.api.testutils.TestDB
import no.nav.syfo.api.testutils.UserConstants
import no.nav.syfo.api.testutils.dropData
import no.nav.syfo.api.testutils.testEnvironment
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.util.pollAndPersist
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.sql.SQLException
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

object PersistenceUtilsSpek : Spek({
    val embeddedKafkaEnvironment = KafkaEnvironment(
        autoStart = false,
        topicNames = listOf("apen-isyfo-stoppautomatikk")
    )

    val sykmeldtFnr = UserConstants.SYKMELDT_FNR
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
    val enhetNr = EnhetNr("9999")

    val env = testEnvironment(
        kafkaBrokersURL = embeddedKafkaEnvironment.brokersURL,
    )

    val partition = 0
    val stoppAutomatikkTopicPartition = TopicPartition(env.stoppAutomatikkTopic, partition)

    val arsakList = listOf(
        Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING),
        Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV)
    )

    val incomingStatusEndring = StatusEndring(
        UUID.randomUUID().toString(),
        veilederIdent,
        sykmeldtFnr,
        Status.STOPP_AUTOMATIKK,
        arsakList,
        primaryJob,
        Instant.now().atZone(ZoneOffset.UTC).toOffsetDateTime(),
        enhetNr
    )
    val hendelse = objectMapper.writeValueAsString(incomingStatusEndring)
    val hendelseRecord = ConsumerRecord(env.stoppAutomatikkTopic, partition, 1, "something", hendelse)

    fun verifyEmptyDB(database: DatabaseInterface) {
        val statusendringListe: List<StatusEndring> = database.getActiveFlags(sykmeldtFnr)
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

        it("Store in database after reading from kafka") {
            pollAndPersist(mockConsumer, database, env)

            val statusendringListe: List<StatusEndring> = database.getActiveFlags(sykmeldtFnr)
            statusendringListe.size shouldBeEqualTo 1

            val statusEndring = statusendringListe[0]
            statusEndring.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
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

            val statusendringListe: List<StatusEndring> = database.getActiveFlags(sykmeldtFnr)
            statusendringListe.size shouldBeEqualTo 1

            val statusEndring = statusendringListe.first()
            statusEndring.arsakList shouldBeEqualTo arsakList
            statusEndring.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
            statusEndring.veilederIdent shouldBeEqualTo veilederIdent
            statusEndring.virksomhetNr shouldBeEqualTo primaryJob
            statusEndring.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
            statusEndring.opprettet.dayOfMonth shouldBeEqualTo
                Instant.now().atZone(ZoneOffset.UTC).toOffsetDateTime().dayOfMonth
            statusEndring.enhetNr shouldBeEqualTo enhetNr

            COUNT_ENDRE_PERSON_STATUS_DB_ALREADY_STORED.get() shouldBeEqualTo 1.0
        }

        it("Catch thrown exception when storing in database fails, then move on") {
            mockkStatic("no.nav.syfo.QueriesKt")
            every { database.addStatus(any(), any(), any(), any(), any(), any(), any()) } throws SQLException("Sql er feil")

            pollAndPersist(mockConsumer, database, env)

            verifyEmptyDB(database)
            COUNT_ENDRE_PERSON_STATUS_DB_FAILED.get() shouldBeEqualTo 1.0
        }
    }
})
