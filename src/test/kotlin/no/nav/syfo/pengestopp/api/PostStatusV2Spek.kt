package no.nav.syfo.pengestopp.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.InternalCoroutinesApi
import no.nav.syfo.objectMapper
import no.nav.syfo.pengestopp.*
import no.nav.syfo.pengestopp.kafka.*
import no.nav.syfo.testutils.*
import no.nav.syfo.testutils.UserConstants.SYKMELDT_FNR
import no.nav.syfo.testutils.UserConstants.SYKMELDT_FNR_IKKE_TILGANG
import no.nav.syfo.util.bearerHeader
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*

@InternalCoroutinesApi
class PostStatusV2Spek : Spek({
    val sykmeldtFnr = SYKMELDT_FNR
    val sykmeldtFnrIkkeTilgang = SYKMELDT_FNR_IKKE_TILGANG
    val veilederIdent = VeilederIdent("Z999999")
    val primaryJob = VirksomhetNr("888")
    val enhetNr = EnhetNr("9999")

    val externalMockEnvironment = ExternalMockEnvironment()
    val database = externalMockEnvironment.database

    val env = externalMockEnvironment.environment

    val testConsumerProperties = kafkaPersonFlaggetAivenConsumerProperties(env).overrideForTest()
        .apply {
            remove(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)
            remove(ConsumerConfig.GROUP_ID_CONFIG)
        }.apply {
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.GROUP_ID_CONFIG, "spek.integration-consumer")
        }
    val testConsumer = KafkaConsumer<String, String>(testConsumerProperties)
    testConsumer.subscribe(listOf(env.stoppAutomatikkAivenTopic))

    val prodConsumerProperties = kafkaPersonFlaggetAivenConsumerProperties(env).overrideForTest()
        .apply {
            remove(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)
            remove(ConsumerConfig.GROUP_ID_CONFIG)
        }.apply {
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.GROUP_ID_CONFIG, "prodConsumer")
        }
    val prodConsumer = KafkaConsumer<String, String>(prodConsumerProperties)
    prodConsumer.subscribe(listOf(env.stoppAutomatikkAivenTopic))

    val producerProperties = kafkaPersonFlaggetAivenProducerProperties(env).overrideForTest()
    val personFlagget84Producer = KafkaProducer<String, StatusEndring>(producerProperties)

    fun withTestApplicationForApi(
        testApp: TestApplicationEngine,
        testDB: TestDB,
        block: TestApplicationEngine.() -> Unit
    ) {
        testApp.start()

        launchKafkaTask(
            applicationState = externalMockEnvironment.applicationState,
            database = testDB,
            environment = env,
            personFlagget84Consumer = prodConsumer,
        )

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
                        val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, null, listOf(primaryJob), enhetNr)
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
                            StoppAutomatikk(sykmeldtFnrIkkeTilgang, null, listOf(primaryJob), enhetNr)
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
                        val stoppAutomatikk = StoppAutomatikk(sykmeldtFnr, null, listOf(primaryJob), enhetNr)
                        val stoppAutomatikkJson = objectMapper.writeValueAsString(stoppAutomatikk)
                        setBody(stoppAutomatikkJson)
                    }
                ) {
                    response.status() shouldBe HttpStatusCode.Created
                }

                val messages: MutableList<StatusEndring> = mutableListOf()

                testConsumer.poll(Duration.ofMillis(5000)).forEach {
                    val hendelse: StatusEndring =
                        objectMapper.readValue(it.value())
                    messages.add(hendelse)
                }

                messages.size shouldBeEqualTo 1

                val latestFlaggperson84Hendelse = messages.last()
                latestFlaggperson84Hendelse.arsakList.shouldBeNull()
                latestFlaggperson84Hendelse.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
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
                            sykmeldtFnr,
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

                val messages: MutableList<StatusEndring> = mutableListOf()

                testConsumer.poll(Duration.ofMillis(5000)).forEach {
                    val hendelse: StatusEndring =
                        objectMapper.readValue(it.value())
                    messages.add(hendelse)
                }

                messages.size shouldBeEqualTo 1

                val latestFlaggperson84Hendelse = messages.last()
                latestFlaggperson84Hendelse.arsakList shouldBeEqualTo arsakList
                latestFlaggperson84Hendelse.sykmeldtFnr shouldBeEqualTo sykmeldtFnr
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
