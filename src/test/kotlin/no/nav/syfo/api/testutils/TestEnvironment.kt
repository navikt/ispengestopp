package no.nav.syfo.api.testutils

import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.application.ApplicationState
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

fun testEnvironment(
    kafkaBrokersURL: String,
    syfotilgangskontrollUrl: String = "http://syfotilgangskontroll",
): Environment {
    return Environment(
        applicationName = "ispengestopp",
        applicationPort = 8080,
        kafkaBootstrapServers = kafkaBrokersURL,
        databaseMountPathVault = "",
        databaseName = "",
        ispengestoppDBURL = "",
        aadDiscoveryUrl = "",
        developmentMode = false,
        loginserviceClientId = "1234",
        stoppAutomatikkTopic = "apen-isyfo-stoppautomatikk",
        syfotilgangskontrollUrl = syfotilgangskontrollUrl,
        pollTimeOutMs = 0L
    )
}

fun testAppState() = ApplicationState(
    alive = AtomicBoolean(true),
    ready = AtomicBoolean(true),
)

fun testVaultSecrets() = VaultSecrets(
    "",
    ""
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}

fun Properties.overrideForTest(): Properties = apply {
    remove("security.protocol")
    remove("sasl.mechanism")
}
