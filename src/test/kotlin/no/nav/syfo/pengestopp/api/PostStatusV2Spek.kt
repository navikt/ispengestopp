package no.nav.syfo.pengestopp.api

import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.InternalCoroutinesApi
import no.nav.syfo.objectMapper
import no.nav.syfo.pengestopp.*
import no.nav.syfo.testutils.*
import no.nav.syfo.testutils.UserConstants.SYKMELDT_PERSONIDENT
import no.nav.syfo.testutils.UserConstants.SYKMELDT_PERSONIDENT_IKKE_TILGANG
import no.nav.syfo.util.bearerHeader
import org.amshove.kluent.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*
import java.util.concurrent.Future

@InternalCoroutinesApi
class PostStatusV2Spek : Spek({
    val sykmeldtPersonIdent = SYKMELDT_PERSONIDENT
    val sykmeldtPersonIdentIkkeTilgang = SYKMELDT_PERSONIDENT_IKKE_TILGANG
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
    val enhetNr = EnhetNr("9999")

    val externalMockEnvironment = ExternalMockEnvironment()
    val database = externalMockEnvironment.database

    val personFlagget84Producer = mockk<KafkaProducer<String, StatusEndring>>(relaxed = true)

    fun withTestApplicationForApi(
        testApp: TestApplicationEngine,
        testDB: TestDB,
        block: TestApplicationEngine.() -> Unit
    ) {
        testApp.start()

        testApp.application.testApiModule(
            externalMockEnvironment = externalMockEnvironment,
            personFlagget84Producer = personFlagget84Producer,
        )

        afterGroup {
            externalMockEnvironment.stopExternalMocks()
        }

        beforeEachTest {
            clearAllMocks()
            coEvery { personFlagget84Producer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
        }

        afterEachTest {
            testDB.connection.dropData()
        }

        return testApp.block()
    }

    describe("Flag a person to be removed from automatic processing") {
        withTestApplicationForApi(TestApplicationEngine(), database) {
            val endpointPath = "$apiV2BasePath$apiV2PersonFlaggPath"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azureAppClientId,
                issuer = externalMockEnvironment.wellKnownInternADV2Mock.issuer,
            )
            it("reject post request without token") {
                with(
                    handleRequest(HttpMethod.Post, endpointPath) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        val stoppAutomatikk = StoppAutomatikk(sykmeldtPersonIdent, null, listOf(primaryJob), enhetNr)
                        val stoppAutomatikkJson = objectMapper.writeValueAsString(stoppAutomatikk)
                        setBody(stoppAutomatikkJson)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.Unauthorized
                }
            }
            it("reject post request to forbidden user") {
                with(
                    handleRequest(HttpMethod.Post, endpointPath) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        val stoppAutomatikk =
                            StoppAutomatikk(sykmeldtPersonIdentIkkeTilgang, null, listOf(primaryJob), enhetNr)
                        val stoppAutomatikkJson = objectMapper.writeValueAsString(stoppAutomatikk)
                        setBody(stoppAutomatikkJson)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.Forbidden
                }
            }
            it("persist status change without ArsakList to kafka and database") {
                with(
                    handleRequest(HttpMethod.Post, endpointPath) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        val stoppAutomatikk = StoppAutomatikk(sykmeldtPersonIdent, null, listOf(primaryJob), enhetNr)
                        val stoppAutomatikkJson = objectMapper.writeValueAsString(stoppAutomatikk)
                        setBody(stoppAutomatikkJson)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.Created
                }

                val producerRecordSlot = slot<ProducerRecord<String, StatusEndring>>()
                verify(exactly = 1) { personFlagget84Producer.send(capture(producerRecordSlot)) }
                val latestFlaggperson84Hendelse = producerRecordSlot.captured.value()

                latestFlaggperson84Hendelse.arsakList.shouldBeNull()
                latestFlaggperson84Hendelse.sykmeldtFnr shouldBeEqualTo sykmeldtPersonIdent
                latestFlaggperson84Hendelse.veilederIdent shouldBeEqualTo veilederIdent
                latestFlaggperson84Hendelse.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
                latestFlaggperson84Hendelse.opprettet.dayOfMonth shouldBeEqualTo Instant.now()
                    .atZone(ZoneOffset.UTC).dayOfMonth
                latestFlaggperson84Hendelse.enhetNr shouldBeEqualTo enhetNr
                latestFlaggperson84Hendelse.virksomhetNr shouldBeEqualTo primaryJob
            }

            it("persist status change with ArsakList to kafka and database") {
                val arsakList = listOf(
                    Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING),
                    Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV)
                )
                with(
                    handleRequest(HttpMethod.Post, endpointPath) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        val stoppAutomatikk = StoppAutomatikk(
                            sykmeldtPersonIdent,
                            arsakList,
                            listOf(primaryJob),
                            enhetNr
                        )
                        val stoppAutomatikkJson = objectMapper.writeValueAsString(stoppAutomatikk)
                        setBody(stoppAutomatikkJson)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.Created
                }

                val producerRecordSlot = slot<ProducerRecord<String, StatusEndring>>()
                verify(exactly = 1) { personFlagget84Producer.send(capture(producerRecordSlot)) }
                val latestFlaggperson84Hendelse = producerRecordSlot.captured.value()

                latestFlaggperson84Hendelse.arsakList shouldBeEqualTo arsakList
                latestFlaggperson84Hendelse.sykmeldtFnr shouldBeEqualTo sykmeldtPersonIdent
                latestFlaggperson84Hendelse.veilederIdent shouldBeEqualTo veilederIdent
                latestFlaggperson84Hendelse.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
                latestFlaggperson84Hendelse.opprettet.dayOfMonth shouldBeEqualTo Instant.now()
                    .atZone(ZoneOffset.UTC).dayOfMonth
                latestFlaggperson84Hendelse.enhetNr shouldBeEqualTo enhetNr
                latestFlaggperson84Hendelse.virksomhetNr shouldBeEqualTo primaryJob
            }
        }
    }
})
