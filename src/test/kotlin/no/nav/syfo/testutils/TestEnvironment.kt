package no.nav.syfo.testutils

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

fun testEnvironment(
    azureTokenEndpoint: String = "azureTokenEndpoint",
    kafkaBrokersURL: String,
    syfotilgangskontrollUrl: String = "http://syfotilgangskontroll",
): Environment {
    return Environment(
        applicationName = "ispengestopp",
        applicationPort = 8080,
        azureAppClientId = "app-client-id",
        azureAppClientSecret = "app-secret",
        azureAppWellKnownUrl = "wellknownurl",
        azureTokenEndpoint = azureTokenEndpoint,
        ispengestoppDbHost = "localhost",
        ispengestoppDbPort = "5432",
        ispengestoppDbName = "isnarmesteleder_dev",
        ispengestoppDbUsername = "username",
        ispengestoppDbPassword = "password",
        kafkaBootstrapServers = kafkaBrokersURL,
        kafkaSchemaRegistryUrl = "http://kafka-schema-registry.tpa.svc.nais.local:8081",
        developmentMode = false,
        stoppAutomatikkTopic = "apen-isyfo-stoppautomatikk",
        syfotilgangskontrollClientId = "syfotilgangskontrollclientid",
        syfotilgangskontrollUrl = syfotilgangskontrollUrl,
        serviceuserUsername = "",
        serviceuserPassword = "",
        pollTimeOutMs = 0L,
    )
}

fun testAppState() = ApplicationState(
    alive = AtomicBoolean(true),
    ready = AtomicBoolean(true),
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}

fun Properties.overrideForTest(): Properties = apply {
    remove("security.protocol")
    remove("sasl.mechanism")
}
