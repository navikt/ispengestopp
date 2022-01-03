package no.nav.syfo.pengestopp.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.server.testing.*
import no.nav.syfo.objectMapper
import no.nav.syfo.pengestopp.*
import no.nav.syfo.pengestopp.database.addStatus
import no.nav.syfo.pengestopp.kafka.kafkaPersonFlaggetConsumerProperties
import no.nav.syfo.pengestopp.kafka.kafkaPersonFlaggetProducerProperties
import no.nav.syfo.testutils.*
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime

class GetStatusV2Spek : Spek({

    val sykmeldtFnr = UserConstants.SYKMELDT_FNR
    val sykmeldtFnrFiller = SykmeldtFnr("654321")
    val sykmeldtFnrIkkeTilgang = UserConstants.SYKMELDT_FNR_IKKE_TILGANG
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
    val secondaryJob = VirksomhetNr("999")
    val enhetNr = EnhetNr("9999")
    val opprettet = OffsetDateTime.now()

    val externalMockEnvironment = ExternalMockEnvironment()
    val database = externalMockEnvironment.database

    val consumerProperties = kafkaPersonFlaggetConsumerProperties(
        externalMockEnvironment.environment,
    )
        .overrideForTest()
    val consumer = KafkaConsumer<String, String>(consumerProperties)
    consumer.subscribe(listOf(externalMockEnvironment.environment.stoppAutomatikkTopic))

    val producerProperties = kafkaPersonFlaggetProducerProperties(
        externalMockEnvironment.environment,
    )
    val personFlagget84Producer = KafkaProducer<String, StatusEndring>(producerProperties)

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

        beforeGroup {
            externalMockEnvironment.startExternalMocks()
        }

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
                        addHeader(NAV_PERSONIDENT_HEADER, sykmeldtFnr.value)
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
                        addHeader(NAV_PERSONIDENT_HEADER, sykmeldtFnrIkkeTilgang.value)
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

                val statusList = listOf(
                    DBStatusChangeTest(
                        "1",
                        sykmeldtFnr,
                        veilederIdent,
                        Status.STOPP_AUTOMATIKK,
                        arsakList,
                        primaryJob,
                        enhetNr,
                        opprettet,
                    ),
                    DBStatusChangeTest(
                        "2",
                        sykmeldtFnr,
                        veilederIdent,
                        Status.STOPP_AUTOMATIKK,
                        arsakList,
                        primaryJob,
                        enhetNr,
                        opprettet,
                    ),
                    DBStatusChangeTest(
                        "3",
                        sykmeldtFnr,
                        veilederIdent,
                        Status.STOPP_AUTOMATIKK,
                        arsakList,
                        secondaryJob,
                        enhetNr,
                        opprettet,
                    ),
                    DBStatusChangeTest(
                        "4",
                        sykmeldtFnrFiller,
                        veilederIdent,
                        Status.STOPP_AUTOMATIKK,
                        arsakList,
                        primaryJob,
                        enhetNr,
                        opprettet,
                    )
                )
                statusList.forEach {
                    database.addStatus(
                        it.uuid,
                        it.sykmeldtFnr,
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
                        addHeader(NAV_PERSONIDENT_HEADER, sykmeldtFnr.value)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.OK

                    val flags: List<StatusEndring> = objectMapper.readValue(response.content!!)

                    flags.size shouldBeEqualTo 3
                    flags.first().sykmeldtFnr.value shouldBeEqualTo sykmeldtFnr.value
                    flags.first().arsakList shouldBeEqualTo arsakList
                    flags.first().opprettet.toEpochSecond()
                        .shouldBeGreaterOrEqualTo(flags.last().opprettet.toEpochSecond())
                }
            }
        }
    }
})