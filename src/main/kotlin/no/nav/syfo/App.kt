package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.InternalCoroutinesApi
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.metric.METRICS_REGISTRY
import no.nav.syfo.common.tilgangskontroll.client.TilgangskontrollClient
import no.nav.syfo.common.token.azuread.AzureAdClient
import no.nav.syfo.infrastructure.database.Database
import no.nav.syfo.infrastructure.database.DatabaseConfig
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.wellknown.getWellKnown
import no.nav.syfo.infrastructure.kafka.identhendelse.IdenthendelseService
import no.nav.syfo.infrastructure.kafka.identhendelse.IdenthendelseConsumerService
import no.nav.syfo.infrastructure.kafka.identhendelse.launchKafkaTaskIdenthendelse
import no.nav.syfo.infrastructure.database.PengestoppRepository
import no.nav.syfo.infrastructure.kafka.StatusEndringProducer
import no.nav.syfo.infrastructure.kafka.aktivitetskrav.launchKafkaTaskAktivitetskrav
import no.nav.syfo.infrastructure.kafka.arbeidsuforhet.launchKafkaTaskArbeidsuforhet
import no.nav.syfo.infrastructure.kafka.createPersonFlagget84AivenConsumer
import no.nav.syfo.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.infrastructure.kafka.manglendemedvirkning.launchKafkaTaskManglendeMedvirkning
import no.nav.syfo.util.configure
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val objectMapper: ObjectMapper = jacksonObjectMapper().configure()

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.AppKt")

const val applicationPort = 8080

@InternalCoroutinesApi
fun main() {
    val environment = Environment()

    // Wire METRICS_REGISTRY into Micrometer's global registry so that counters registered
    // on Metrics.globalRegistry (e.g. by shared libraries like isyfo-backend-common) are
    // also exposed at /internal/metrics and scraped by Prometheus.
    Metrics.addRegistry(METRICS_REGISTRY)

    val applicationState = ApplicationState()
    val database = Database(
        DatabaseConfig(
            jdbcUrl = environment.jdbcUrl(),
            username = environment.ispengestoppDbUsername,
            password = environment.ispengestoppDbPassword,
        )
    )

    val wellKnownInternADV2 = getWellKnown(
        wellKnownUrl = environment.azure.appWellKnownUrl,
    )

    val azureAdClient = AzureAdClient()

    val pdlClient = PdlClient(
        systemTokenProvider = azureAdClient,
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
            val pengestoppService = PengestoppService(
                pengestoppRepository = pengestoppRepository,
                statusEndringProducer = statusEndringProducer,
            )
            apiModule(
                applicationState = applicationState,
                database = database,
                env = environment,
                wellKnownInternADV2 = wellKnownInternADV2,
                pengestoppService = pengestoppService,
                tilgangskontrollClient = TilgangskontrollClient(
                    oboTokenProvider = azureAdClient,
                    clientConfig = environment.tilgangskontroll,
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
                launchKafkaTaskManglendeMedvirkning(
                    applicationState = applicationState,
                    environment = environment,
                    pengestoppService = pengestoppService,
                )
                launchKafkaTaskArbeidsuforhet(
                    applicationState = applicationState,
                    environment = environment,
                    pengestoppService = pengestoppService,
                )
                launchKafkaTaskAktivitetskrav(
                    applicationState = applicationState,
                    environment = environment,
                    pengestoppService = pengestoppService,
                )
            }
            monitor.subscribe(ApplicationStopPreparing) {
                applicationState.ready = false
            }
        }
    )

    server.start(wait = true)
}
