package no.nav.syfo.pengestopp.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.syfo.pengestopp.*
import no.nav.syfo.pengestopp.database.addStatus
import no.nav.syfo.testutils.*
import no.nav.syfo.testutils.generator.generateStatusEndringer
import no.nav.syfo.util.*
import org.amshove.kluent.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class GetStatusV2Spek : Spek({

    val sykmeldtPersonIdent = UserConstants.SYKMELDT_PERSONIDENT
    val sykmeldtPersonIdentIkkeTilgang = UserConstants.SYKMELDT_PERSONIDENT_IKKE_TILGANG

    val externalMockEnvironment = ExternalMockEnvironment()
    val database = externalMockEnvironment.database

    val personFlagget84Producer = mockk<KafkaProducer<String, StatusEndring>>()

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
                Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING),
                Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV)
            )
            val statusList = generateStatusEndringer(
                arsakList = arsakList,
            )
            statusList.forEach {
                database.addStatus(
                    it.uuid,
                    it.personIdent,
                    it.veilederIdent,
                    it.enhetNr,
                    it.arsakList,
                    it.virksomhetNr,
                    it.opprettet,
                )
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
    }
})
