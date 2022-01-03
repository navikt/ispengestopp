package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.authentication.*
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.client.wellknown.WellKnown
import no.nav.syfo.pengestopp.StatusEndring
import no.nav.syfo.pengestopp.api.registerFlaggPerson84V2
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

    val azureAdClient = AzureAdClient(
        azureAppClientId = env.azureAppClientId,
        azureAppClientSecret = env.azureAppClientSecret,
        azureTokenEndpoint = env.azureTokenEndpoint,
    )

    val tilgangskontrollConsumer = TilgangskontrollConsumer(
        azureAdClient = azureAdClient,
        syfotilgangskontrollClientId = env.syfotilgangskontrollClientId,
        tilgangskontrollBaseUrl = env.syfotilgangskontrollUrl,
    )

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
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