package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.objectMapper
import org.apache.kafka.common.serialization.Serializer

class JacksonKafkaSerializer : Serializer<Any> {
    override fun serialize(topic: String?, data: Any?): ByteArray = objectMapper.writeValueAsBytes(data)
    override fun close() {}
}
