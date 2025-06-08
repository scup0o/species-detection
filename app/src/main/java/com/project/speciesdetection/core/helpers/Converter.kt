package com.project.speciesdetection.core.helpers

import com.google.firebase.Timestamp
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