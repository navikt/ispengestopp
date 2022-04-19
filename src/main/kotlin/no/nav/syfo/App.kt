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
import no.nav.syfo.application.database.Database
import no.nav.syfo.application.database.DatabaseConfig
import no.nav.syfo.client.wellknown.getWellKnown
import no.nav.syfo.pengestopp.kafka.*
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

    val personFlagget84Producer = createPersonFlagget84Producer(environment)
    val personFlagget84Consumer = createPersonFlagget84Consumer(environment)

    val wellKnownInternADV2 = getWellKnown(
        wellKnownUrl = environment.azureAppWellKnownUrl,
    )

    val applicationEngineEnvironment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = applicationPort
        }
        module {
            apiModule(
                applicationState = applicationState,
                database = database,
                env = environment,
                personFlagget84Producer = personFlagget84Producer,
                wellKnownInternADV2 = wellKnownInternADV2,
            )
        }
    }

    val server = embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment,
    )

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) { application ->
        applicationState.ready = true
        application.environment.log.info("Application is ready")
        log.info("Hello from ispengestopp")

        launchKafkaTask(
            applicationState = applicationState,
            database = database,
            environment = environment,
            personFlagget84Consumer = personFlagget84Consumer,
        )
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = false)
}
