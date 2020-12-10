package no.nav.syfo.application

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.Environment
import no.nav.syfo.StatusEndring
import no.nav.syfo.api.registerFlaggPerson84
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.TimeUnit

fun createApplicationEngine(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    env: Environment,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>
): ApplicationEngine =
    embeddedServer(Netty, env.applicationPort) {
        val log = LoggerFactory.getLogger("ktor.application")
        // TODO Her kan man tydeligvis også installere CallID (SyfooversiktApplication.kt linje 202)
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }

        val jwkProvider = JwkProviderBuilder(URL(env.jwksUri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

        setupAuth(env, jwkProvider)

        routing {
            registerNaisApi(applicationState)
            authenticate {
                registerFlaggPerson84(database, env, personFlagget84Producer, TilgangskontrollConsumer())
            }
        }
    }
