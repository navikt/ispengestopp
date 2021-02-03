package no.nav.syfo

data class Environment(
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "ispengestopp"),
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),

    val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),

    val databaseMountPathVault: String = getEnvVar("DATABASE_MOUNT_PATH_VAULT"),
    val databaseName: String = getEnvVar("DATABASE_NAME", "ispengestopp"),
    val ispengestoppDBURL: String = getEnvVar("ISPENGESTOPP_DB_URL"),
    val developmentMode: Boolean = getEnvVar("DEVELOPMENT_MODE", "false").toBoolean(),
    val aadDiscoveryUrl: String = getEnvVar("AADDISCOVERY_URL"),
    val loginserviceClientId: String = getEnvVar("LOGINSERVICE_CLIENT_ID", "1234"),
    val stoppAutomatikkTopic: String = getEnvVar("STOPP_AUTOMATIKK_TOPIC", "apen-isyfo-stoppautomatikk"),
    val pollTimeOutMs: Long = 0

)

data class VaultSecrets(
    val serviceuserUsername: String,
    val serviceuserPassword: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
