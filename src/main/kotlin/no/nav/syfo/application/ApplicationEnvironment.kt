package no.nav.syfo.application

import no.nav.syfo.common.token.azuread.AzureAdClientConfig
import no.nav.syfo.common.util.ClientConfig
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment

data class Environment(
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "ispengestopp"),

    val azure: AzureAdClientConfig = AzureAdClientConfig.fromEnv(),

    val ispengestoppDbHost: String = getEnvVar("NAIS_DATABASE_ISPENGESTOPP_ISPENGESTOPP_DB_HOST"),
    val ispengestoppDbPort: String = getEnvVar("NAIS_DATABASE_ISPENGESTOPP_ISPENGESTOPP_DB_PORT"),
    val ispengestoppDbName: String = getEnvVar("NAIS_DATABASE_ISPENGESTOPP_ISPENGESTOPP_DB_DATABASE"),
    val ispengestoppDbUsername: String = getEnvVar("NAIS_DATABASE_ISPENGESTOPP_ISPENGESTOPP_DB_USERNAME"),
    val ispengestoppDbPassword: String = getEnvVar("NAIS_DATABASE_ISPENGESTOPP_ISPENGESTOPP_DB_PASSWORD"),

    val pdlClientId: String = getEnvVar("PDL_CLIENT_ID"),
    val pdlUrl: String = getEnvVar("PDL_URL"),

    val developmentMode: Boolean = getEnvVar("DEVELOPMENT_MODE", "false").toBoolean(),
    val tilgangskontroll: ClientConfig = ClientConfig(
        baseUrl = getEnvVar("ISTILGANGSKONTROLL_URL"),
        clientId = getEnvVar("ISTILGANGSKONTROLL_CLIENT_ID"),
    ),
    val pollTimeOutMs: Long = 0,
    val stoppAutomatikkAivenTopic: String = "teamsykefravr.apen-isyfo-stoppautomatikk",
    val kafka: KafkaEnvironment = KafkaEnvironment(
        aivenBootstrapServers = getEnvVar("KAFKA_BROKERS"),
        aivenCredstorePassword = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
        aivenKeystoreLocation = getEnvVar("KAFKA_KEYSTORE_PATH"),
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
        aivenSchemaRegistryUrl = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
        aivenRegistryUser = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
        aivenRegistryPassword = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
    ),
) {
    fun jdbcUrl(): String {
        return "jdbc:postgresql://$ispengestoppDbHost:$ispengestoppDbPort/$ispengestoppDbName"
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
