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
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class GetStatusV2Test {

    private val sykmeldtPersonIdent = UserConstants.SYKMELDT_PERSONIDENT
    private val sykmeldtPersonIdentIkkeTilgang = UserConstants.SYKMELDT_PERSONIDENT_IKKE_TILGANG

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val pengestoppRepository = PengestoppRepository(database = database)

    private val pengestoppService = PengestoppService(
        pengestoppRepository = pengestoppRepository,
        statusEndringProducer = StatusEndringProducer(
            environment = externalMockEnvironment.environment,
            kafkaProducer = mockk<KafkaProducer<String, StatusEndring>>()
        ),
    )

    private fun ApplicationTestBuilder.setupApiAndClient(): HttpClient {
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

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    private val endpointPath = "$apiV2BasePath$apiV2PersonStatusPath"
    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azureAppClientId,
        issuer = externalMockEnvironment.wellKnownInternADV2Mock.issuer,
    )

    @Test
    fun `reject request without bearer token`() {
        testApplication {
            val client = setupApiAndClient()
            val response = client.get(endpointPath) {
                header(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdent.value)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `reject request to forbidden user`() {
        testApplication {
            val client = setupApiAndClient()
            val response = client.get(endpointPath) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, sykmeldtPersonIdentIkkeTilgang.value)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `return correct content`() {
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
            assertEquals(HttpStatusCode.OK, response.status)

            val flags: List<StatusEndring> = response.body()

            assertEquals(3, flags.size)
            assertEquals(sykmeldtPersonIdent.value, flags.first().sykmeldtFnr.value)
            assertEquals(arsakList, flags.first().arsakList)
            assertTrue(flags.first().opprettet.toEpochSecond() >= flags.last().opprettet.toEpochSecond())
        }
    }

    @Test
    fun `returns statusendring without arsaker`() {
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
            assertEquals(HttpStatusCode.OK, response.status)

            val flags: List<StatusEndring> = response.body()

            assertEquals(3, flags.size)
            assertEquals(sykmeldtPersonIdent.value, flags.first().sykmeldtFnr.value)
            assertTrue(flags.all { it.arsakList.isEmpty() })
        }
    }

    @Test
    fun `returns no statusendring when it was created automatically`() {
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
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `return correct content - remove statusEndringer with only deprecated arsaker`() {
        val opprettet = OffsetDateTime.of(2025, 2, 10, 0, 0, 0, 0, ZoneOffset.UTC)

        val statusList = listOf(
            generateStatusEndring(
                arsakList = listOf(
                    Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING),
                    Arsak(type = SykepengestoppArsak.MEDISINSK_VILKAR)
                ),
                opprettet = opprettet.plusSeconds(10),
            ),
            generateStatusEndring(
                arsakList = listOf(
                    Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV)
                ),
                opprettet = opprettet.plusSeconds(9),
            ),
            generateStatusEndring(
                arsakList = listOf(
                    Arsak(type = SykepengestoppArsak.TILBAKEDATERT_SYKMELDING)
                ),
                opprettet = opprettet.plusSeconds(8),
            ),
            generateStatusEndring(
                arsakList = emptyList(),
                opprettet = opprettet.plusSeconds(7),
            ),
            generateStatusEndring(
                arsakList = listOf(
                    Arsak(type = SykepengestoppArsak.BESTRIDELSE_SYKMELDING)
                ),
                opprettet = opprettet,
            ),
            generateStatusEndring(
                arsakList = listOf(
                    Arsak(type = SykepengestoppArsak.TILBAKEDATERT_SYKMELDING),
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
            assertEquals(HttpStatusCode.OK, response.status)

            val flags: List<StatusEndring> = response.body()

            assertEquals(3, flags.size)
            val deprecatedArsakRemoved = flags[0]
            assertEquals(sykmeldtPersonIdent.value, deprecatedArsakRemoved.sykmeldtFnr.value)
            assertEquals(listOf(Arsak(SykepengestoppArsak.MEDISINSK_VILKAR)), deprecatedArsakRemoved.arsakList)

            val aktivitetskravOnly = flags[1]
            assertTrue(deprecatedArsakRemoved.opprettet.toEpochSecond() >= aktivitetskravOnly.opprettet.toEpochSecond())

            assertEquals(sykmeldtPersonIdent.value, aktivitetskravOnly.sykmeldtFnr.value)
            assertEquals(listOf(Arsak(SykepengestoppArsak.AKTIVITETSKRAV)), aktivitetskravOnly.arsakList)
            assertTrue(aktivitetskravOnly.opprettet.toEpochSecond() <= deprecatedArsakRemoved.opprettet.toEpochSecond())

            val arsakslisteEmpty = flags[2]
            assertTrue(aktivitetskravOnly.opprettet.toEpochSecond() >= arsakslisteEmpty.opprettet.toEpochSecond())

            assertEquals(sykmeldtPersonIdent.value, arsakslisteEmpty.sykmeldtFnr.value)
            assertEquals(emptyList<Arsak>(), arsakslisteEmpty.arsakList)
            assertTrue(arsakslisteEmpty.opprettet.toEpochSecond() <= aktivitetskravOnly.opprettet.toEpochSecond())
        }
    }
}
