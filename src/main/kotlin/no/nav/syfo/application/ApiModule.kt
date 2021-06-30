package no.nav.syfo.application

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.Environment
import no.nav.syfo.StatusEndring
import no.nav.syfo.api.registerFlaggPerson84
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.application.authentication.*
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.database.DatabaseInterface
import org.apache.kafka.clients.producer.KafkaProducer

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    env: Environment,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>,
    wellKnownInternADV1: WellKnown
) {
    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                accectedAudienceList = listOf(env.loginserviceClientId),
                jwtIssuerType = JwtIssuerType.INTERN_AZUREAD_V1,
                wellKnown = wellKnownInternADV1,
            )
        )
    )
    installCallId()
    installContentNegotiation()
    installStatusPages()

    val tilgangskontrollConsumer = TilgangskontrollConsumer(
        tilgangskontrollBaseUrl = env.syfotilgangskontrollUrl
    )

    routing {
        registerNaisApi(applicationState)
        authenticate(JwtIssuerType.INTERN_AZUREAD_V1.name) {
            registerFlaggPerson84(
                database,
                env,
                personFlagget84Producer,
                tilgangskontrollConsumer
            )
        }
    }
}
