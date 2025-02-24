package no.nav.syfo.pengestopp.api

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.InternalCoroutinesApi
import no.nav.syfo.pengestopp.*
import no.nav.syfo.testutils.*
import no.nav.syfo.testutils.UserConstants.SYKMELDT_PERSONIDENT
import no.nav.syfo.testutils.UserConstants.SYKMELDT_PERSONIDENT_IKKE_TILGANG
import no.nav.syfo.util.*
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

    fun ApplicationTestBuilder.setupApiAndClient(): HttpClient {
        application {
            testApiModule(
                externalMockEnvironment = externalMockEnvironment,
                personFlagget84Producer = personFlagget84Producer,
            )
        }
        val client = createClient {
            install(ContentNegotiation) {
                jackson { configure() }
            }
        }
        return client
    }

    afterGroup {
        externalMockEnvironment.stopExternalMocks()
        database.stop()
    }

    afterEachTest {
        database.connection.dropData()
    }

    beforeEachTest {
        clearAllMocks()
        coEvery { personFlagget84Producer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
    }

    describe("Flag a person to be removed from automatic processing") {
        val endpointPath = "$apiV2BasePath$apiV2PersonFlaggPath"
        val validToken = generateJWT(
            audience = externalMockEnvironment.environment.azureAppClientId,
            issuer = externalMockEnvironment.wellKnownInternADV2Mock.issuer,
        )
        val stoppAutomatikk = StoppAutomatikk(sykmeldtPersonIdent, emptyList(), listOf(primaryJob), enhetNr)

        it("reject post request without token") {
            testApplication {
                val client = setupApiAndClient()

                val response = client.post(endpointPath) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(stoppAutomatikk)
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
        it("reject post request to forbidden user") {
            testApplication {
                val client = setupApiAndClient()

                val response = client.post(endpointPath) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(stoppAutomatikk.copy(sykmeldtFnr = sykmeldtPersonIdentIkkeTilgang))
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }
        it("persist status change without ArsakList to kafka and database") {
            testApplication {
                val client = setupApiAndClient()

                val response = client.post(endpointPath) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(stoppAutomatikk)
                }
                response.status shouldBe HttpStatusCode.Created

                val producerRecordSlot = slot<ProducerRecord<String, StatusEndring>>()
                verify(exactly = 1) { personFlagget84Producer.send(capture(producerRecordSlot)) }
                val latestFlaggperson84Hendelse = producerRecordSlot.captured.value()

                latestFlaggperson84Hendelse.arsakList.shouldBeEmpty()
                latestFlaggperson84Hendelse.sykmeldtFnr shouldBeEqualTo sykmeldtPersonIdent
                latestFlaggperson84Hendelse.veilederIdent shouldBeEqualTo veilederIdent
                latestFlaggperson84Hendelse.status shouldBeEqualTo Status.STOPP_AUTOMATIKK
                latestFlaggperson84Hendelse.opprettet.dayOfMonth shouldBeEqualTo Instant.now()
                    .atZone(ZoneOffset.UTC).dayOfMonth
                latestFlaggperson84Hendelse.enhetNr shouldBeEqualTo enhetNr
                latestFlaggperson84Hendelse.virksomhetNr shouldBeEqualTo primaryJob
            }
        }

        it("persist status change with ArsakList to kafka and database") {
            testApplication {
                val client = setupApiAndClient()
                val arsakList = listOf(
                    Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING),
                    Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV)
                )

                val response = client.post(endpointPath) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        StoppAutomatikk(
                            sykmeldtPersonIdent,
                            arsakList,
                            listOf(primaryJob),
                            enhetNr
                        )
                    )
                }
                response.status shouldBe HttpStatusCode.Created

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
