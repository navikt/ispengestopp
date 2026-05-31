package no.nav.syfo.testutils

import io.ktor.server.application.*
import no.nav.syfo.application.PengestoppService
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.common.tilgangskontroll.client.TilgangskontrollClient
import no.nav.syfo.common.token.azuread.AzureAdClient

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
    pengestoppService: PengestoppService,
) {
    val azureAdClient = AzureAdClient(
        config = externalMockEnvironment.environment.azure,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        env = externalMockEnvironment.environment,
        wellKnownInternADV2 = externalMockEnvironment.wellKnownInternADV2Mock,
        pengestoppService = pengestoppService,
        tilgangskontrollClient = TilgangskontrollClient(
            oboTokenProvider = azureAdClient,
            clientConfig = externalMockEnvironment.environment.tilgangskontroll,
            httpClient = externalMockEnvironment.mockHttpClient,
        ),
    )
}
