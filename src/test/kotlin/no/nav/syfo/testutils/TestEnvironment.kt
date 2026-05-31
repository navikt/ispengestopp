package no.nav.syfo.testutils

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.common.token.azuread.AzureAdClientConfig
import no.nav.syfo.common.util.ClientConfig
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment
import java.util.*

fun testEnvironment(): Environment {
    return Environment(
        applicationName = "ispengestopp",
        azure = AzureAdClientConfig(
            appClientId = "app-client-id",
            appClientSecret = "app-secret",
            appWellKnownUrl = "wellknownurl",
            openidConfigTokenEndpoint = "azureTokenEndpoint",
        ),
        ispengestoppDbHost = "localhost",
        ispengestoppDbPort = "5432",
        ispengestoppDbName = "isnarmesteleder_dev",
        ispengestoppDbUsername = "username",
        ispengestoppDbPassword = "password",
        developmentMode = false,
        tilgangskontroll = ClientConfig(
            baseUrl = "tilgangskontrollUrl",
            clientId = "tilgangskontrollclientid",
        ),
        pollTimeOutMs = 0L,
        pdlClientId = "dev-fss.pdl.pdl-api",
        pdlUrl = "pdlUrl",
        kafka = KafkaEnvironment(
            aivenBootstrapServers = "kafkaBrokersURL",
            aivenCredstorePassword = "credstorepassord",
            aivenKeystoreLocation = "keystore",
            aivenSecurityProtocol = "SSL",
            aivenTruststoreLocation = "truststore",
            aivenSchemaRegistryUrl = "http://kafka-schema-registry.tpa.svc.nais.local:8081",
            aivenRegistryUser = "registryuser",
            aivenRegistryPassword = "registrypassword",
        ),
    )
}

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)
