package no.nav.syfo.application

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.auth.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.Environment
import no.nav.syfo.StatusEndring
import no.nav.syfo.api.registerFlaggPerson84
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.database.DatabaseInterface
import org.apache.kafka.clients.producer.KafkaProducer
import java.net.URL
import java.util.concurrent.TimeUnit

fun createApplicationEngine(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    env: Environment,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>,
    tilgangskontrollConsumer: TilgangskontrollConsumer
): ApplicationEngine =
    embeddedServer(Netty, env.applicationPort) {
        installCallId()
        installContentNegotiation()
        installStatusPages()

        val jwkProvider = JwkProviderBuilder(URL(env.jwksUri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

        setupAuth(env, jwkProvider)

        routing {
            registerNaisApi(applicationState)
            authenticate {
                registerFlaggPerson84(
                    database,
                    env,
                    personFlagget84Producer,
                    tilgangskontrollConsumer
                )
            }
        }
    }
