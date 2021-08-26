package no.nav.syfo.application

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.Environment
import no.nav.syfo.StatusEndring
import no.nav.syfo.api.registerFlaggPerson84V2
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.application.authentication.JwtIssuer
import no.nav.syfo.application.authentication.JwtIssuerType
import no.nav.syfo.application.authentication.WellKnown
import no.nav.syfo.application.authentication.installJwtAuthentication
import no.nav.syfo.client.azuread.v2.AzureAdV2Client
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.database.DatabaseInterface
import org.apache.kafka.clients.producer.KafkaProducer

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    env: Environment,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>,
    wellKnownInternADV2: WellKnown,
) {
    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(env.azureAppClientId),
                jwtIssuerType = JwtIssuerType.INTERN_AZUREAD_V2,
                wellKnown = wellKnownInternADV2,
            ),
        )
    )
    installCallId()
    installContentNegotiation()
    installStatusPages()

    val azureAdV2Client = AzureAdV2Client(
        azureAppClientId = env.azureAppClientId,
        azureAppClientSecret = env.azureAppClientSecret,
        azureTokenEndpoint = env.azureTokenEndpoint,
    )

    val tilgangskontrollConsumer = TilgangskontrollConsumer(
        azureAdV2Client = azureAdV2Client,
        syfotilgangskontrollClientId = env.syfotilgangskontrollClientId,
        tilgangskontrollBaseUrl = env.syfotilgangskontrollUrl,
    )

    routing {
        registerNaisApi(applicationState)
        authenticate(JwtIssuerType.INTERN_AZUREAD_V2.name) {
            registerFlaggPerson84V2(
                database,
                env,
                personFlagget84Producer,
                tilgangskontrollConsumer
            )
        }
    }
}
