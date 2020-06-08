package no.nav.syfo

import no.nav.syfo.kafka.KafkaConfig
import no.nav.syfo.kafka.KafkaCredentials

data class Environment(
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "ispengestopp"),
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),

    override val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val sm2013ManuellBehandlingTopic: String = getEnvVar("KAFKA_SM2013_MANUELL_BEHANDLING", "privat-syfo-sm2013-manuellBehandling"),
    val sm2013AutomatiskBehandlingTopic: String = getEnvVar("KAFKA_SM2013_AUTOMATISK_BEHANDLING_TOPIC", "privat-syfo-sm2013-automatiskBehandling"),
    val smregisterRecievedSykmeldingBackupTopic: String = getEnvVar("KAFKA_SMREGISTER_RECIEVED_SYKMELDING_BACKUP_TOPIC", "privat-syfosmregister-received-sykmelding-backup"),
    val sm2013BehandlingsutfallTopic: String = getEnvVar("KAFKA_SM2013_BEHANDLINGSUTFALL_TOPIC", "privat-syfo-sm2013-behandlingsUtfall"),
    val smregisterBehandlingsutfallBackupTopic: String = getEnvVar("KAFKA_SMREGISTER_BEHANDLINGSUTFALL_BACKUP_TOPIC", "privat-syfosmregister-behandlingsutfall-backup"),
    val syfoSykmeldingstatusLeesahTopic: String = getEnvVar("KAFKA_SYFO_SYKMELDINGSTATUS_LEESAH_TOPIC", "aapen-syfo-sykmeldingstatus-leesah-v1"),
    val syfoRegisterStatusBackupTopic: String = getEnvVar("KAFKA_SYFO_REGISTER_STATUS_BACKUP_TOPIC", "privat-syfo-register-status-backup"),
    val kafkaConsumerTopics: List<String> = listOf(sm2013ManuellBehandlingTopic, sm2013AutomatiskBehandlingTopic, smregisterRecievedSykmeldingBackupTopic, sm2013BehandlingsutfallTopic, smregisterBehandlingsutfallBackupTopic, syfoSykmeldingstatusLeesahTopic, syfoRegisterStatusBackupTopic),

    val databaseMountPathVault: String = getEnvVar("DATABASE_MOUNT_PATH_VAULT"),
    val databaseName: String = getEnvVar("DATABASE_NAME", "ispengestopp"),
    val ispengestoppDBURL: String = getEnvVar("ispengestopp_DB_URL"),

    val developmentMode: Boolean = getEnvVar("DEVELOPMENT_MODE", "false").toBoolean()
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
