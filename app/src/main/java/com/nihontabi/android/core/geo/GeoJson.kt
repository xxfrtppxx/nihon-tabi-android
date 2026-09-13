package com.nihontabi.android.core.geo

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Minimal GeoJSON model — just enough to parse the static boundary files
 * served by `/geo/files/...` (Polygon/MultiPolygon rings + a numeric `id`
 * property), not a general-purpose GeoJSON library.
 */
@Serializable
data class GeoFeatureCollection(
    val type: String = "FeatureCollection",
    val features: List<GeoFeature> = emptyList(),
)

@Serializable
data class GeoFeature(
    val type: String = "Feature",
    val properties: GeoFeatureProperties = GeoFeatureProperties(),
    val geometry: GeoGeometry,
)

@Serializable
data class GeoFeatureProperties(
    val id: Int? = null,
)

typealias Ring = List<List<Double>>
typealias Polygon = List<Ring>

/**
 * A ring is a list of `[lng, lat]` pairs; a Polygon is a list of rings
 * (first = shell, rest = holes); a MultiPolygon is a list of Polygons.
 * Normalized to [polygons] (always a list-of-polygons) regardless of the
 * GeoJSON `type`, since a `coordinates` array's nesting depth depends on a
 * sibling field's value — something plain `@Serializable` field mapping
 * can't express, hence the custom [KSerializer] below.
 */
@Serializable(with = GeoGeometrySerializer::class)
data class GeoGeometry(
    val type: String,
    val polygons: List<Polygon>,
)

object GeoGeometrySerializer : KSerializer<GeoGeometry> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.nihontabi.android.core.geo.GeoGeometry")

    override fun deserialize(decoder: Decoder): GeoGeometry {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("GeoGeometry can only be deserialized from JSON")
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        val type = obj["type"]?.jsonPrimitive?.content ?: "Unknown"
        val coordinates = obj["coordinates"]?.jsonArray ?: JsonArray(emptyList())

        val polygons: List<Polygon> = when (type) {
            "Polygon" -> listOf(coordinates.toPolygon())
            "MultiPolygon" -> coordinates.map { it.jsonArray.toPolygon() }
            else -> emptyList()
        }
        return GeoGeometry(type, polygons)
    }

    override fun serialize(encoder: Encoder, value: GeoGeometry) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("GeoGeometry can only be serialized to JSON")
        val coordinates = when (value.type) {
            "Polygon" -> value.polygons.firstOrNull()?.polygonToJson() ?: buildJsonArray {}
            "MultiPolygon" -> buildJsonArray { value.polygons.forEach { add(it.polygonToJson()) } }
            else -> buildJsonArray {}
        }
        val element = buildJsonObject {
            put("type", value.type)
            put("coordinates", coordinates)
        }
        jsonEncoder.encodeJsonElement(element)
    }

    private fun Polygon.polygonToJson() = buildJsonArray { forEach { ring -> add(ring.ringToJson()) } }
    private fun Ring.ringToJson() = buildJsonArray { forEach { point -> add(point.pointToJson()) } }
    private fun List<Double>.pointToJson() = buildJsonArray { forEach { add(JsonPrimitive(it)) } }

    private fun JsonArray.toPolygon(): Polygon = map { ring -> ring.jsonArray.toRing() }
    private fun JsonArray.toRing(): Ring = map { point -> point.jsonArray.toPoint() }
    private fun JsonArray.toPoint(): List<Double> = map { (it as JsonPrimitive).content.toDouble() }
}
