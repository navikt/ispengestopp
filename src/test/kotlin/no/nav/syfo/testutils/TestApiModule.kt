package no.nav.syfo.testutils

import io.ktor.server.application.*
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.client.azuread.AzureAdClient
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
    )
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        env = externalMockEnvironment.environment,
        personFlagget84Producer = personFlagget84Producer,
        wellKnownInternADV2 = externalMockEnvironment.wellKnownInternADV2Mock,
        azureAdClient = azureAdClient,
    )
}
