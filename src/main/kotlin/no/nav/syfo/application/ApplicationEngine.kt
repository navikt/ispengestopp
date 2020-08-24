package no.nav.syfo.application

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.Environment
import no.nav.syfo.KFlaggperson84Hendelse
import no.nav.syfo.api.registerFlaggPerson84
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.database.DatabaseInterface
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.util.OffsetDateTimeConverter
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

fun createApplicationEngine(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    env: Environment,
    personFlagget84Producer: KafkaProducer<String, KFlaggperson84Hendelse>
): ApplicationEngine =
    embeddedServer(Netty, env.applicationPort) {
        val log = LoggerFactory.getLogger("ktor.application")
        // TODO Her kan man tydeligvis også installere CallID (SyfooversiktApplication.kt linje 202)
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
                registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeConverter())
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
