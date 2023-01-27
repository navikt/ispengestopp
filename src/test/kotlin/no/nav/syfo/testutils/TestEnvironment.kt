package no.nav.syfo.testutils

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.kafka.KafkaEnvironment
import java.net.ServerSocket
import java.util.*

fun testEnvironment(
    azureTokenEndpoint: String = "azureTokenEndpoint",
    kafkaBrokersURL: String,
    syfotilgangskontrollUrl: String = "http://syfotilgangskontroll",
): Environment {
    return Environment(
        applicationName = "ispengestopp",
        azureAppClientId = "app-client-id",
        azureAppClientSecret = "app-secret",
        azureAppWellKnownUrl = "wellknownurl",
        azureTokenEndpoint = azureTokenEndpoint,
        ispengestoppDbHost = "localhost",
        ispengestoppDbPort = "5432",
        ispengestoppDbName = "isnarmesteleder_dev",
        ispengestoppDbUsername = "username",
        ispengestoppDbPassword = "password",
        developmentMode = false,
        syfotilgangskontrollClientId = "syfotilgangskontrollclientid",
        syfotilgangskontrollUrl = syfotilgangskontrollUrl,
        serviceuserUsername = "",
        serviceuserPassword = "",
        pollTimeOutMs = 0L,
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
        toggleKafkaConsumerIdenthendelseEnabled = true,
    )
}

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}

fun Properties.overrideForTest(): Properties = apply {
    remove("security.protocol")
    remove("sasl.mechanism")
}
