package no.nav.syfo.api.testutils

import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import java.net.ServerSocket

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
        jwtIssuer = "https://sts.issuer.net/myid",
        jwksUri = "src/test/resources/jwkset.json",
        developmentMode = false,
        loginserviceClientId = "1234",
        stoppAutomatikkTopic = "apen-isyfo-stoppautomatikk",
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
