package no.nav.syfo.testutils

import io.ktor.server.application.*
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.tilgangskontroll.TilgangskontrollClient

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
    pengestoppService: PengestoppService,
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
        wellKnownInternADV2 = externalMockEnvironment.wellKnownInternADV2Mock,
        pengestoppService = pengestoppService,
        tilgangskontrollClient = TilgangskontrollClient(
            azureAdClient = azureAdClient,
            tilgangskontrollClientId = externalMockEnvironment.environment.tilgangskontrollClientId,
            tilgangskontrollBaseUrl = externalMockEnvironment.environment.tilgangskontrollUrl,
            httpClient = externalMockEnvironment.mockHttpClient,
        ),
    )
}
