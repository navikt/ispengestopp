package no.nav.syfo.application

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.Environment
import no.nav.syfo.StatusEndring
import no.nav.syfo.api.registerFlaggPerson84
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.application.authentication.WellKnown
import no.nav.syfo.application.authentication.installAuthentication
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.database.DatabaseInterface
import org.apache.kafka.clients.producer.KafkaProducer

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    env: Environment,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>,
    tilgangskontrollConsumer: TilgangskontrollConsumer,
    wellKnown: WellKnown
) {
    installAuthentication(
        wellKnown,
        listOf(
            env.loginserviceClientId
        )
    )
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
