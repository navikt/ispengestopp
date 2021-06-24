package no.nav.syfo.api.testutils

import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import java.net.ServerSocket
import java.util.*

fun testEnvironment(
    kafkaBrokersURL: String
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
        syfotilgangskontrollUrl = "http://syfotilgangskontroll",
        pollTimeOutMs = 0L
    )
}

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
