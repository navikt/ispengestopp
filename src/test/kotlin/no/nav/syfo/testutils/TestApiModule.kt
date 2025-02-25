package no.nav.syfo.testutils

import io.ktor.server.application.*
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollClient
import no.nav.syfo.infrastructure.database.PengestoppRepository
import no.nav.syfo.pengestopp.StatusEndring
import org.apache.kafka.clients.producer.KafkaProducer

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
    personFlagget84Producer: KafkaProducer<String, StatusEndring>,
) {
    val azureAdClient = AzureAdClient(
        azureAppClientId = externalMockEnvironment.environment.azureAppClientId,
        azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
        azureTokenEndpoint = externalMockEnvironment.environment.azureTokenEndpoint,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        env = externalMockEnvironment.environment,
        personFlagget84Producer = personFlagget84Producer,
        wellKnownInternADV2 = externalMockEnvironment.wellKnownInternADV2Mock,
        pengestoppRepository = PengestoppRepository(database = externalMockEnvironment.database),
        tilgangskontrollClient = TilgangskontrollClient(
            azureAdClient = azureAdClient,
            tilgangskontrollClientId = externalMockEnvironment.environment.tilgangskontrollClientId,
            tilgangskontrollBaseUrl = externalMockEnvironment.environment.tilgangskontrollUrl,
            httpClient = externalMockEnvironment.mockHttpClient,
        ),
    )
}
