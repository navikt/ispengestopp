package no.nav.syfo.pengestopp.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.infrastructure.database.PengestoppRepository
import no.nav.syfo.infrastructure.kafka.StatusEndringProducer
import no.nav.syfo.pengestopp.Arsak
import no.nav.syfo.pengestopp.StatusEndring
import no.nav.syfo.pengestopp.SykepengestoppArsak
import no.nav.syfo.testutils.*
import no.nav.syfo.testutils.generator.generateAutomaticStatusEndring
import no.nav.syfo.testutils.generator.generateStatusEndring
import no.nav.syfo.testutils.generator.generateStatusEndringer
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.configure
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.time.ZoneOffset

class GetStatusV2Spek : Spek({

    val sykmeldtPersonIdent = UserConstants.SYKMELDT_PERSONIDENT
    val sykmeldtPersonIdentIkkeTilgang = UserConstants.SYKMELDT_PERSONIDENT_IKKE_TILGANG

    val externalMockEnvironment = ExternalMockEnvironment()
    val database = externalMockEnvironment.database
    val pengestoppRepository = PengestoppRepository(database = database)

    val pengestoppService = PengestoppService(
        pengestoppRepository = pengestoppRepository,
        statusEndringProducer = StatusEndringProducer(
            environment = externalMockEnvironment.environment,
            kafkaProducer = mockk<KafkaProducer<String, StatusEndring>>()
        ),
    )

    fun ApplicationTestBuilder.setupApiAndClient(): HttpClient {
        application {
            testApiModule(
                pengestoppService = pengestoppService,
                externalMockEnvironment = externalMockEnvironment,
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

    describe("Get person flag status") {

        val endpointPath = "$apiV2BasePath$apiV2PersonStatusPath"
        val validToken = generateJWT(
            audience = externalMockEnvironment.environment.azureAppClientId,
            issuer = externalMockEnvironment.wellKnownInternADV2Mock.issuer,
        )
        it("reject request without bearer token") {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(endpointPath) {
                    header(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdent.value)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
        it("reject request to forbidden user") {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(endpointPath) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdentIkkeTilgang.value)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }
        it("return correct content") {
            val arsakList = listOf(
                Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV),
            )
            val statusList = generateStatusEndringer(
                arsakList = arsakList,
                opprettet = OffsetDateTime.of(2025, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC),
            )
            statusList.forEach {
                pengestoppRepository.createStatusEndring(statusEndring = it)
            }
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(endpointPath) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdent.value)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                response.status shouldBe HttpStatusCode.OK

                val flags: List<StatusEndring> = response.body()

                flags.size shouldBeEqualTo 3
                flags.first().sykmeldtFnr.value shouldBeEqualTo sykmeldtPersonIdent.value
                flags.first().arsakList shouldBeEqualTo arsakList
                flags.first().opprettet.toEpochSecond()
                    .shouldBeGreaterOrEqualTo(flags.last().opprettet.toEpochSecond())
            }
        }

        it("returns statusendring without arsaker") {
            val statusEndringer = generateStatusEndringer(
                arsakList = emptyList(),
                opprettet = OffsetDateTime.now(),
            )
            statusEndringer.forEach {
                pengestoppRepository.createStatusEndring(statusEndring = it)
            }

            testApplication {
                val client = setupApiAndClient()
                val response = client.get(endpointPath) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdent.value)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                response.status shouldBe HttpStatusCode.OK

                val flags: List<StatusEndring> = response.body()

                flags.size shouldBeEqualTo 3
                flags.first().sykmeldtFnr.value shouldBeEqualTo sykmeldtPersonIdent.value
                flags.all { it.arsakList.isEmpty() } shouldBeEqualTo true
            }
        }

        it("returns no statusendring when it was created automatically") {
            val statusEndring = generateAutomaticStatusEndring(
                arsakList = listOf(Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV))
            )
            pengestoppRepository.createStatusEndring(statusEndring)

            testApplication {
                val client = setupApiAndClient()
                val response = client.get(endpointPath) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdent.value)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        it("return correct content - remove statusEndringer with only deprecated arsaker") {
            val opprettet = OffsetDateTime.of(2025, 2, 10, 0, 0, 0, 0, ZoneOffset.UTC)

            val statusList = listOf(
                generateStatusEndring(
                    arsakList = listOf(
                        Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING),
                        Arsak(type = SykepengestoppArsak.MEDISINSK_VILKAR)
                    ),
                    opprettet = opprettet.plusSeconds(3),
                ),
                generateStatusEndring(
                    arsakList = listOf(
                        Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV)
                    ),
                    opprettet = opprettet.plusSeconds(2),
                ),
                generateStatusEndring(
                    arsakList = listOf(
                        Arsak(type = SykepengestoppArsak.TILBAKEDATERT_SYKMELDING)
                    ),
                    opprettet = opprettet.plusSeconds(1),
                ),
                generateStatusEndring(
                    arsakList = listOf(
                        Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING)
                    ),
                    opprettet = opprettet,
                ),
            )
            statusList.forEach {
                pengestoppRepository.createStatusEndring(statusEndring = it)
            }
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(endpointPath) {
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdent.value)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                response.status shouldBe HttpStatusCode.OK

                val flags: List<StatusEndring> = response.body()

                flags.size shouldBeEqualTo 2
                flags.first().sykmeldtFnr.value shouldBeEqualTo sykmeldtPersonIdent.value
                flags.first().arsakList shouldBeEqualTo listOf(Arsak(SykepengestoppArsak.MEDISINSK_VILKAR))
                flags.first().opprettet.toEpochSecond()
                    .shouldBeGreaterOrEqualTo(flags.last().opprettet.toEpochSecond())

                flags.last().sykmeldtFnr.value shouldBeEqualTo sykmeldtPersonIdent.value
                flags.last().arsakList shouldBeEqualTo listOf(Arsak(SykepengestoppArsak.AKTIVITETSKRAV))
                flags.last().opprettet.toEpochSecond()
                    .shouldBeLessOrEqualTo(flags.first().opprettet.toEpochSecond())
            }
        }
    }
})
