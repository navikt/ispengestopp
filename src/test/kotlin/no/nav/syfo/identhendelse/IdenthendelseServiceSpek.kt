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
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object IdenthendelseServiceSpek : Spek({
    describe(IdenthendelseServiceSpek::class.java.simpleName) {
        val externalMockEnvironment = ExternalMockEnvironment()
        val database = externalMockEnvironment.database
        val azureAdClient = AzureAdClient(
            azureAppClientId = externalMockEnvironment.environment.azureAppClientId,
            azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
            azureTokenEndpoint = externalMockEnvironment.environment.azureTokenEndpoint,
            httpClient = externalMockEnvironment.mockHttpClient,
        )
        val pdlClient = PdlClient(
            azureAdClient = azureAdClient,
            pdlClientId = externalMockEnvironment.environment.pdlClientId,
            pdlUrl = externalMockEnvironment.environment.pdlUrl,
            httpClient = externalMockEnvironment.mockHttpClient
        )

        val repository = PengestoppRepository(database = database)
        val identhendelseService = IdenthendelseService(
            pengestoppRepository = repository,
            pdlClient = pdlClient,
        )

        afterGroup {
            externalMockEnvironment.stopExternalMocks()
        }

        afterEachTest {
            database.connection.dropData()
        }

        describe("Happy path") {
            it("Skal oppdatere statusendring når person har fått ny ident") {
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
                updatedStatusEndring.size shouldBeEqualTo 3
                updatedStatusEndring.first().sykmeldtFnr shouldBeEqualTo newIdent

                val oldStatusEndring = repository.getStatusEndringer(oldIdent)
                oldStatusEndring.size shouldBeEqualTo 0
            }
        }

        describe("Unhappy path") {
            it("Skal kaste feil hvis PDL ikke har oppdatert identen") {
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
                    assertFailsWith(IllegalStateException::class) {
                        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                    }
                }
            }
        }
    }
})
