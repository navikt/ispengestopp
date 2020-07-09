package no.nav.syfo.kafka

import com.google.gson.Gson
import org.apache.kafka.common.serialization.Serializer

class GsonKafkaSerializer : Serializer<Any> {
    private val gsonMapper = Gson()
    override fun serialize(topic: String?, data: Any?): ByteArray = gsonMapper.toJson(data).toByteArray()
    override fun close() {}
}
