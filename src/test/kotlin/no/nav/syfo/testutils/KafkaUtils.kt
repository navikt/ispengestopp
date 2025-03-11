package no.nav.syfo.testutils

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition

fun <T> mockRecords(records: List<T>): ConsumerRecords<String, T> {
    val consumerRecords = records.mapIndexed { index, record ->
        ConsumerRecord("topic", 0, index.toLong(), "key$index", record)
    }
    return ConsumerRecords(mapOf(TopicPartition("topic", 0) to consumerRecords))
}
