package no.nav.syfo

import no.nav.syfo.kafka.KafkaConfig
import no.nav.syfo.kafka.KafkaCredentials

data class Environment(
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "ispengestopp"),
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),

    override val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),

    val databaseMountPathVault: String = getEnvVar("DATABASE_MOUNT_PATH_VAULT"),
    val databaseName: String = getEnvVar("DATABASE_NAME", "ispengestopp"),
    val ispengestoppDBURL: String = getEnvVar("ISPENGESTOPP_DB_URL"),
    val jwtIssuer: String = getEnvVar("JWT_ISSUER"),
    val jwksUri: String = getEnvVar("JWKS_URI", "https://login.microsoftonline.com/common/discovery/keys"),
    val developmentMode: Boolean = getEnvVar("DEVELOPMENT_MODE", "false").toBoolean(),
    val loginserviceClientId: String = getEnvVar("LOGINSERVICE_CLIENT_ID", "1234"),
    val stoppAutomatikkTopic: String = getEnvVar("STOPP_AUTOMATIKK_TOPIC", "apen-isyfo-stoppautomatikk")
) : KafkaConfig

data class VaultSecrets(
    val serviceuserUsername: String,
    val serviceuserPassword: String
) : KafkaCredentials {
    override val kafkaUsername: String = serviceuserUsername
    override val kafkaPassword: String = serviceuserPassword
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
