package no.nav.syfo

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.*
import no.nav.syfo.application.authentication.getWellKnown
import no.nav.syfo.database.*
import no.nav.syfo.kafka.createPersonFlagget84Consumer
import no.nav.syfo.kafka.createPersonFlagget84Producer
import no.nav.syfo.util.pollAndPersist
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .registerKotlinModule()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.BootstrapKt")

@InternalCoroutinesApi
@KtorExperimentalAPI
fun main() {
    val env = Environment()

    val applicationState = ApplicationState()
    val database = Database(
        DatabaseConfig(
            jdbcUrl = env.jdbcUrl(),
            username = env.ispengestoppDbUsername,
            password = env.ispengestoppDbPassword,
        )
    )

    val personFlagget84Producer = createPersonFlagget84Producer(env)
    val personFlagget84Consumer = createPersonFlagget84Consumer(env)

    val wellKnownInternADV2 = getWellKnown(env.azureAppWellKnownUrl)

    val applicationEngine = embeddedServer(Netty, env.applicationPort) {
        apiModule(
            applicationState = applicationState,
            database = database,
            env = env,
            personFlagget84Producer = personFlagget84Producer,
            wellKnownInternADV2 = wellKnownInternADV2,
        )
    }

    val applicationServer = ApplicationServer(applicationEngine)

    applicationServer.getEnvironment().monitor.subscribe(ApplicationStarted) { application ->
        applicationState.ready.set(true)
        application.environment.log.info("Application is ready")
        log.info("Hello from ispengestopp")

        val toggleKafkaConsumerEnabled = env.toggleKafkaConsumerEnabled
        if (toggleKafkaConsumerEnabled) {
            launchListeners(
                applicationState,
                database,
                personFlagget84Consumer,
                env
            )
        }
    }
    applicationServer.start()
}

fun createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (e: Exception) {
            log.error(
                "En uhåndtert feil oppstod, applikasjonen restarter {}",
                StructuredArguments.fields(e.message),
                e.cause
            )
        } finally {
            applicationState.alive.set(false)
        }
    }

@InternalCoroutinesApi
@KtorExperimentalAPI
fun launchListeners(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    personFlagget84Consumer: KafkaConsumer<String, String>,
    env: Environment
) {
    createListener(applicationState) {
        applicationState.ready.set(true)

        blockingApplicationLogic(
            applicationState,
            database,
            personFlagget84Consumer,
            env
        )
    }
}

@KtorExperimentalAPI
suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    personFlagget84Consumer: KafkaConsumer<String, String>,
    env: Environment
) {
    while (applicationState.ready.get()) {
        pollAndPersist(personFlagget84Consumer, database, env)
        delay(100)
    }
}
