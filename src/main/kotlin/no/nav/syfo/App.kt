package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.InternalCoroutinesApi
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.infrastructure.database.Database
import no.nav.syfo.infrastructure.database.DatabaseConfig
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollClient
import no.nav.syfo.client.wellknown.getWellKnown
import no.nav.syfo.infrastructure.kafka.identhendelse.IdenthendelseService
import no.nav.syfo.infrastructure.kafka.identhendelse.IdenthendelseConsumerService
import no.nav.syfo.infrastructure.kafka.identhendelse.launchKafkaTaskIdenthendelse
import no.nav.syfo.infrastructure.database.PengestoppRepository
import no.nav.syfo.infrastructure.kafka.StatusEndringProducer
import no.nav.syfo.infrastructure.kafka.createPersonFlagget84AivenConsumer
import no.nav.syfo.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.util.configure
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

val objectMapper: ObjectMapper = jacksonObjectMapper().configure()

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.AppKt")

const val applicationPort = 8080

@InternalCoroutinesApi
fun main() {
    val environment = Environment()

    val applicationState = ApplicationState()
    val database = Database(
        DatabaseConfig(
            jdbcUrl = environment.jdbcUrl(),
            username = environment.ispengestoppDbUsername,
            password = environment.ispengestoppDbPassword,
        )
    )

    val wellKnownInternADV2 = getWellKnown(
        wellKnownUrl = environment.azureAppWellKnownUrl,
    )

    val azureAdClient = AzureAdClient(
        azureAppClientId = environment.azureAppClientId,
        azureAppClientSecret = environment.azureAppClientSecret,
        azureTokenEndpoint = environment.azureTokenEndpoint,
    )

    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        pdlUrl = environment.pdlUrl,
        pdlClientId = environment.pdlClientId,
    )

    val statusEndringProducer = StatusEndringProducer(
        environment = environment,
    )

    val applicationEnvironment = applicationEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())
    }
    val server = embeddedServer(
        Netty,
        environment = applicationEnvironment,
        configure = {
            connector {
                port = applicationPort
            }
            connectionGroupSize = 8
            workerGroupSize = 8
            callGroupSize = 16
        },
        module = {
            val pengestoppRepository = PengestoppRepository(database = database)
            apiModule(
                applicationState = applicationState,
                database = database,
                env = environment,
                statusEndringProducer = statusEndringProducer,
                wellKnownInternADV2 = wellKnownInternADV2,
                pengestoppRepository = pengestoppRepository,
                tilgangskontrollClient = TilgangskontrollClient(
                    azureAdClient = azureAdClient,
                    tilgangskontrollClientId = environment.tilgangskontrollClientId,
                    tilgangskontrollBaseUrl = environment.tilgangskontrollUrl,
                ),
            )
            monitor.subscribe(ApplicationStarted) {
                applicationState.ready = true
                log.info("Application is ready, running Java VM ${Runtime.version()}")

                launchKafkaTask(
                    applicationState = applicationState,
                    pengestoppRepository = pengestoppRepository,
                    environment = environment,
                    personFlagget84Consumer = createPersonFlagget84AivenConsumer(environment),
                )

                val identhendelseService = IdenthendelseService(
                    pengestoppRepository = pengestoppRepository,
                    pdlClient = pdlClient,
                )
                val kafkaIdenthendelseConsumerService = IdenthendelseConsumerService(
                    identhendelseService = identhendelseService,
                )
                launchKafkaTaskIdenthendelse(
                    applicationState = applicationState,
                    environment = environment,
                    kafkaIdenthendelseConsumerService = kafkaIdenthendelseConsumerService,
                )
            }
        }
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = true)
}
