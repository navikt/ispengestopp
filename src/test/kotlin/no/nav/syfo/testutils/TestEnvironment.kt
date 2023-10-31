package no.nav.syfo.testutils

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.kafka.KafkaEnvironment
import java.util.*

fun testEnvironment(
    kafkaBrokersURL: String,
): Environment {
    return Environment(
        applicationName = "ispengestopp",
        azureAppClientId = "app-client-id",
        azureAppClientSecret = "app-secret",
        azureAppWellKnownUrl = "wellknownurl",
        azureTokenEndpoint = "azureTokenEndpoint",
        ispengestoppDbHost = "localhost",
        ispengestoppDbPort = "5432",
        ispengestoppDbName = "isnarmesteleder_dev",
        ispengestoppDbUsername = "username",
        ispengestoppDbPassword = "password",
        developmentMode = false,
        tilgangskontrollClientId = "tilgangskontrollclientid",
        tilgangskontrollUrl = "tilgangskontrollUrl",
        pollTimeOutMs = 0L,
        pdlClientId = "dev-fss.pdl.pdl-api",
        pdlUrl = "pdlUrl",
        kafka = KafkaEnvironment(
            aivenBootstrapServers = kafkaBrokersURL,
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

fun Properties.overrideForTest(): Properties = apply {
    remove("security.protocol")
    remove("sasl.mechanism")
}
