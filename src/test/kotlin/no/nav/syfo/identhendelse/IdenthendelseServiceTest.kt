package no.nav.syfo.identhendelse

import kotlinx.coroutines.*
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.infrastructure.database.PengestoppRepository
import no.nav.syfo.infrastructure.kafka.identhendelse.IdenthendelseService
import no.nav.syfo.pengestopp.Arsak
import no.nav.syfo.pengestopp.SykepengestoppArsak
import no.nav.syfo.testutils.generator.generateKafkaIdenthendelseDTO
import no.nav.syfo.testutils.*
import no.nav.syfo.testutils.generator.generateStatusEndringer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IdenthendelseServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment()
    private val database = externalMockEnvironment.database
    private val azureAdClient = AzureAdClient(
        azureAppClientId = externalMockEnvironment.environment.azureAppClientId,
        azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
        azureTokenEndpoint = externalMockEnvironment.environment.azureTokenEndpoint,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    private val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        pdlClientId = externalMockEnvironment.environment.pdlClientId,
        pdlUrl = externalMockEnvironment.environment.pdlUrl,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    private val repository = PengestoppRepository(database = database)
    private val identhendelseService = IdenthendelseService(
        pengestoppRepository = repository,
        pdlClient = pdlClient,
    )

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {

        @Test
        fun `Skal oppdatere statusendring når person har fått ny ident`() {
            val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = true)
            val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
            val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

            val arsakList = listOf(Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV))
            val statusList = generateStatusEndringer(
                personIdent = oldIdent,
                arsakList = arsakList,
            )
            statusList.forEach {
                repository.createStatusEndring(statusEndring = it)
            }

            runBlocking {
                identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
            }

            val updatedStatusEndring = repository.getStatusEndringer(newIdent)
            assertEquals(3, updatedStatusEndring.size)
            assertEquals(newIdent, updatedStatusEndring.first().sykmeldtFnr)

            val oldStatusEndring = repository.getStatusEndringer(oldIdent)
            assertEquals(0, oldStatusEndring.size)
        }
    }

    @Nested
    @DisplayName("Unhappy path")
    inner class UnhappyPath {

        @Test
        fun `Skal kaste feil hvis PDL ikke har oppdatert identen`() {
            val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(
                personIdent = UserConstants.SYKMELDT_PERSONIDENT_3,
                hasOldPersonident = true,
            )
            val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

            val arsakList = listOf(Arsak(type = SykepengestoppArsak.AKTIVITETSKRAV))
            val statusList = generateStatusEndringer(
                personIdent = oldIdent,
                arsakList = arsakList,
            )
            statusList.forEach {
                repository.createStatusEndring(statusEndring = it)
            }

            runBlocking {
                assertThrows<IllegalStateException> {
                    identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                }
            }
        }
    }
}
