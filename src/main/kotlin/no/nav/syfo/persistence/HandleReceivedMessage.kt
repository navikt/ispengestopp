package no.nav.syfo.persistence

import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.Environment
import no.nav.syfo.database.Database
import no.nav.syfo.log
import no.nav.syfo.persistence.db.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition

@KtorExperimentalAPI
fun handleReceivedMessage(
    database: Database,
    env: Environment,
    consumerRecord: ConsumerRecord<String, String>,
    endOffsets: Map<TopicPartition, Long>
) {
    try {
        when (consumerRecord.topic()) {
            env.sm2013ManuellBehandlingTopic -> {
                logProgressForPartition(consumerRecord, endOffsets)
                database.createSmManuellBehandling(consumerRecord.value(), consumerRecord.key())
            }
            env.sm2013AutomatiskBehandlingTopic -> {
                logProgressForPartition(consumerRecord, endOffsets)
                database.createSmAutomatiskBehandling(consumerRecord.value(), consumerRecord.key())
            }
            env.smregisterRecievedSykmeldingBackupTopic -> {
                logProgressForPartition(consumerRecord, endOffsets)
                database.createSmHist(consumerRecord.value(), consumerRecord.key())
            }
            env.sm2013BehandlingsutfallTopic -> {
                logProgressForPartition(consumerRecord, endOffsets)
                database.createSmBehandlingsutfall(consumerRecord.value(), consumerRecord.key())
            }
            env.smregisterBehandlingsutfallBackupTopic -> {
                logProgressForPartition(consumerRecord, endOffsets)
                database.createSmBehandlingsutfallHist(consumerRecord.value(), consumerRecord.key())
            }
            env.syfoSykmeldingstatusLeesahTopic -> {
                logProgressForPartition(consumerRecord, endOffsets)
                database.createSmSykmeldingstatus(consumerRecord.value(), consumerRecord.key())
            }
            env.syfoRegisterStatusBackupTopic -> {
                logProgressForPartition(consumerRecord, endOffsets)
                database.createSmSykmeldingstatusHist(consumerRecord.value(), consumerRecord.key())
            }
        }
    } catch (e: Exception) {
        log.error("Noe feilet! :( Skulle lese data med key ${consumerRecord.key()} fra topic: ${consumerRecord.topic()}, for partition ${consumerRecord.partition()} with offset ${consumerRecord.offset()}. ${e.message}")
        throw e
    }
}

fun logProgressForPartition(consumerRecord: ConsumerRecord<String, String>, endOffsets: Map<TopicPartition, Long>) {
    if (consumerRecord.offset() == 0L) {
        log.info("Kafka-trace: Er på offset 0 for ${consumerRecord.topic()}-${consumerRecord.partition()}")
    }

    if (endOffsets.isEmpty()) {
        return
    }

    val topicPartition = TopicPartition(consumerRecord.topic(), consumerRecord.partition())
    val endForThisPartition = endOffsets[topicPartition] ?: return

    val oneQuarter = endForThisPartition / 4
    val oneThird = endForThisPartition / 3
    val halfwayPoint = endForThisPartition / 2
    val twoThirds = (endForThisPartition / 3) * 2
    val threeQuarters = (endForThisPartition / 4) * 3
    val endPoint = endForThisPartition -1

    when (consumerRecord.offset()) {
        oneQuarter -> {
            log.info("Kafka-trace: Er en fjerdedel på vei for ${consumerRecord.topic()}-${consumerRecord.partition()}, med offset ${consumerRecord.offset()}. Endoffsets: $endOffsets")
        }
        oneThird -> {
            log.info("Kafka-trace: Er en tredjedel på vei for ${consumerRecord.topic()}-${consumerRecord.partition()}, med offset ${consumerRecord.offset()}. Endoffsets: $endOffsets")
        }
        halfwayPoint -> {
            log.info("Kafka-trace: Er halvveis for ${consumerRecord.topic()}-${consumerRecord.partition()}, med offset ${consumerRecord.offset()}. Endoffsets: $endOffsets")
        }
        twoThirds -> {
            log.info("Kafka-trace: Er to tredjedeler på vei for ${consumerRecord.topic()}-${consumerRecord.partition()}, med offset ${consumerRecord.offset()}. Endoffsets: $endOffsets")
        }
        threeQuarters -> {
            log.info("Kafka-trace: Er tre fjerdedeler på vei for ${consumerRecord.topic()}-${consumerRecord.partition()}, med offset ${consumerRecord.offset()}. Endoffsets: $endOffsets")
        }
        endPoint -> {
            log.info("Kafka-trace: Er på endOffset for ${consumerRecord.topic()}-${consumerRecord.partition()}, med offset ${consumerRecord.offset()}. Endoffsets: $endOffsets")
        }
    }
}
