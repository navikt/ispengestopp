package no.nav.syfo.pengestopp.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.syfo.objectMapper
import no.nav.syfo.pengestopp.*
import no.nav.syfo.pengestopp.database.addStatus
import no.nav.syfo.testutils.*
import no.nav.syfo.testutils.generator.generateStatusEndringer
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
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

    // TODO: gjøre database delen av testen om til å gi mer test coverage av prodkoden
    fun withTestApplicationForApi(
        testApp: TestApplicationEngine,
        database: TestDB,
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

        afterEachTest {
            database.connection.dropData()
        }

        return testApp.block()
    }

    describe("Get person flag status") {
        afterGroup {
            database.stop()
        }

        withTestApplicationForApi(TestApplicationEngine(), database) {
            val endpointPath = "$apiV2BasePath$apiV2PersonStatusPath"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azureAppClientId,
                issuer = externalMockEnvironment.wellKnownInternADV2Mock.issuer,
            )
            it("reject request without bearer token") {
                with(
                    handleRequest(HttpMethod.Get, endpointPath) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdent.value)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.Unauthorized
                }
            }
            it("reject request to forbidden user") {
                with(
                    handleRequest(HttpMethod.Get, endpointPath) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(Authorization, bearerHeader(validToken))
                        addHeader(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdentIkkeTilgang.value)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.Forbidden
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

                with(
                    handleRequest(HttpMethod.Get, endpointPath) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(Authorization, bearerHeader(validToken))
                        addHeader(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdent.value)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.OK

                    val flags: List<StatusEndring> = objectMapper.readValue(response.content!!)

                    flags.size shouldBeEqualTo 3
                    flags.first().sykmeldtFnr.value shouldBeEqualTo sykmeldtPersonIdent.value
                    flags.first().arsakList shouldBeEqualTo arsakList
                    flags.first().opprettet.toEpochSecond()
                        .shouldBeGreaterOrEqualTo(flags.last().opprettet.toEpochSecond())
                }
            }
        }
    }
})
