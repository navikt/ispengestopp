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
import no.nav.syfo.pengestopp.*
import no.nav.syfo.testutils.*
import no.nav.syfo.testutils.generator.generateStatusEndringer
import no.nav.syfo.util.*
import org.amshove.kluent.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalTime
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

        it("return no content when statusendringer after cutoff date") {
            val arsakList = listOf(
                Arsak(type = SykepengestoppArsak.MANGLENDE_MEDVIRKING),
            )
            val statusList = generateStatusEndringer(
                arsakList = arsakList,
                opprettet = OffsetDateTime.of(LocalDate.of(2025, 3, 10), LocalTime.now(), ZoneOffset.UTC),
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

                response.status shouldBe HttpStatusCode.NoContent
            }
        }
    }
})
