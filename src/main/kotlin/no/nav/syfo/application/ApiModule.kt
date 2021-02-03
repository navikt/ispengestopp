package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.Environment
import no.nav.syfo.StatusEndring
import no.nav.syfo.api.registerFlaggPerson84
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.database.DatabaseInterface
import org.apache.kafka.clients.producer.KafkaProducer

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    env: Environment,
    jwkProvider: JwkProvider,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>,
    tilgangskontrollConsumer: TilgangskontrollConsumer
) {
    installAuthentication(env, jwkProvider)
    installCallId()
    installContentNegotiation()
    installStatusPages()

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
