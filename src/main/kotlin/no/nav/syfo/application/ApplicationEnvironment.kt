package no.nav.syfo.application

import no.nav.syfo.application.kafka.KafkaEnvironment

data class Environment(
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "ispengestopp"),

    val azureAppClientId: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppClientSecret: String = getEnvVar("AZURE_APP_CLIENT_SECRET"),
    val azureTokenEndpoint: String = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    val azureAppWellKnownUrl: String = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),

    val ispengestoppDbHost: String = getEnvVar("NAIS_DATABASE_ISPENGESTOPP_ISPENGESTOPP_DB_HOST"),
    val ispengestoppDbPort: String = getEnvVar("NAIS_DATABASE_ISPENGESTOPP_ISPENGESTOPP_DB_PORT"),
    val ispengestoppDbName: String = getEnvVar("NAIS_DATABASE_ISPENGESTOPP_ISPENGESTOPP_DB_DATABASE"),
    val ispengestoppDbUsername: String = getEnvVar("NAIS_DATABASE_ISPENGESTOPP_ISPENGESTOPP_DB_USERNAME"),
    val ispengestoppDbPassword: String = getEnvVar("NAIS_DATABASE_ISPENGESTOPP_ISPENGESTOPP_DB_PASSWORD"),

    val pdlClientId: String = getEnvVar("PDL_CLIENT_ID"),
    val pdlUrl: String = getEnvVar("PDL_URL"),

    val developmentMode: Boolean = getEnvVar("DEVELOPMENT_MODE", "false").toBoolean(),
    val tilgangskontrollClientId: String = getEnvVar("ISTILGANGSKONTROLL_CLIENT_ID"),
    val tilgangskontrollUrl: String = getEnvVar("ISTILGANGSKONTROLL_URL"),
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
