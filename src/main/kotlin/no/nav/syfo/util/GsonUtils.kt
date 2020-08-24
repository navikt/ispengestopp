package no.nav.syfo.util

import com.google.gson.*
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor


class OffsetDateTimeConverter : JsonSerializer<OffsetDateTime?>, JsonDeserializer<OffsetDateTime?> {
    override fun serialize(src: OffsetDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(FORMATTER.format(src))
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): OffsetDateTime {
        return FORMATTER.parse(json.asJsonPrimitive.asString) { temporal: TemporalAccessor? -> OffsetDateTime.from(temporal) }
    }

    companion object {
        private val FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }
}
