package no.nav.syfo.kafka

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import no.nav.syfo.util.OffsetDateTimeConverter
import org.apache.kafka.common.serialization.Serializer
import java.time.OffsetDateTime

class GsonKafkaSerializer : Serializer<Any> {

    private val gsonMapper: Gson = GsonBuilder()
        .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeConverter())
        .create()

    override fun serialize(topic: String?, data: Any?): ByteArray = gsonMapper.toJson(data).toByteArray()
    override fun close() {}
}
