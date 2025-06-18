package com.project.speciesdetection.core.helpers

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.util.Date

// Tạo custom TimestampSerializer
object TimestampSerializer : KSerializer<Timestamp> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Timestamp) {
        // Chuyển Timestamp thành chuỗi ISO 8601
        encoder.encodeString(value.toDate().toISOString())
    }

    override fun deserialize(decoder: Decoder): Timestamp {
        // Chuyển chuỗi ISO 8601 thành Timestamp
        val isoString = decoder.decodeString()
        val date = Date.from(java.time.Instant.parse(isoString))
        return Timestamp(date)
    }
}

// Extension để convert Date thành ISO string
fun Date.toISOString(): String {
    return this.toInstant().toString()  // Chuyển đổi thành ISO 8601 string
}

object GeoPointSerializer : KSerializer<GeoPoint> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GeoPoint") {
        element<Double>("latitude")
        element<Double>("longitude")
    }

    override fun serialize(encoder: Encoder, value: GeoPoint) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeDoubleElement(descriptor, 0, value.latitude)
        composite.encodeDoubleElement(descriptor, 1, value.longitude)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): GeoPoint {
        val dec = decoder.beginStructure(descriptor)
        var lat = 0.0
        var lng = 0.0

        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> lat = dec.decodeDoubleElement(descriptor, 0)
                1 -> lng = dec.decodeDoubleElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unexpected index $index")
            }
        }

        dec.endStructure(descriptor)
        return GeoPoint(lat, lng)
    }
}