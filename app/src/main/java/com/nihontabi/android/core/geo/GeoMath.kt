package com.nihontabi.android.core.geo

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** `[lng, lat]`. */
data class LngLat(val lng: Double, val lat: Double)

/** `[[minLng, minLat], [maxLng, maxLat]]`. */
data class LngLatBounds(val minLng: Double, val minLat: Double, val maxLng: Double, val maxLat: Double)

/** A point in on-screen/view space (pixels, or any consistent unit). */
data class ViewPoint(val x: Float, val y: Float)

/**
 * Line-for-line port of `nihon-tabi-web/lib/geo.ts` and the `project`/
 * `unproject` helpers in `nihon-tabi-web/components/dot-map.tsx`, so the
 * Android map classifies/projects points identically to the web app.
 */
object GeoMath {

    fun boundsOfFeatureCollection(fc: GeoFeatureCollection): LngLatBounds? {
        var minLng = Double.POSITIVE_INFINITY
        var minLat = Double.POSITIVE_INFINITY
        var maxLng = Double.NEGATIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        for (feature in fc.features) {
            for (polygon in feature.geometry.polygons) {
                for (ring in polygon) {
                    for (point in ring) {
                        val lng = point[0]
                        val lat = point[1]
                        if (lng < minLng) minLng = lng
                        if (lat < minLat) minLat = lat
                        if (lng > maxLng) maxLng = lng
                        if (lat > maxLat) maxLat = lat
                    }
                }
            }
        }
        if (!minLng.isFinite()) return null
        return LngLatBounds(minLng, minLat, maxLng, maxLat)
    }

    fun boundsOfGeometry(geometry: GeoGeometry): LngLatBounds? {
        var minLng = Double.POSITIVE_INFINITY
        var minLat = Double.POSITIVE_INFINITY
        var maxLng = Double.NEGATIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        for (polygon in geometry.polygons) {
            for (ring in polygon) {
                for (point in ring) {
                    val lng = point[0]
                    val lat = point[1]
                    if (lng < minLng) minLng = lng
                    if (lat < minLat) minLat = lat
                    if (lng > maxLng) maxLng = lng
                    if (lat > maxLat) maxLat = lat
                }
            }
        }
        if (!minLng.isFinite()) return null
        return LngLatBounds(minLng, minLat, maxLng, maxLat)
    }

    /** Standard ray-casting point-in-ring test (even-odd rule). */
    private fun pointInRing(lng: Double, lat: Double, ring: Ring): Boolean {
        var inside = false
        var j = ring.size - 1
        for (i in ring.indices) {
            val xi = ring[i][0]
            val yi = ring[i][1]
            val xj = ring[j][0]
            val yj = ring[j][1]
            val intersects = (yi > lat) != (yj > lat) &&
                lng < (xj - xi) * (lat - yi) / (yj - yi) + xi
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }

    /**
     * `[lng, lat]` membership test against a Polygon/MultiPolygon, honoring
     * holes (a polygon's first ring is its shell, further rings are holes).
     */
    fun pointInGeometry(point: LngLat, geometry: GeoGeometry): Boolean {
        fun inPolygon(rings: Polygon): Boolean {
            if (rings.isEmpty() || !pointInRing(point.lng, point.lat, rings[0])) return false
            for (i in 1 until rings.size) {
                if (pointInRing(point.lng, point.lat, rings[i])) return false
            }
            return true
        }
        return geometry.polygons.any { inPolygon(it) }
    }

    /** Great-circle distance between two lat/lng points, in kilometers. */
    fun haversineKm(a: LngLat, b: LngLat): Double {
        val r = 6371.0
        fun toRad(deg: Double) = deg * PI / 180
        val dLat = toRad(b.lat - a.lat)
        val dLng = toRad(b.lng - a.lng)
        val sinLat = sin(dLat / 2)
        val sinLng = sin(dLng / 2)
        val h = sinLat * sinLat + cos(toRad(a.lat)) * cos(toRad(b.lat)) * sinLng * sinLng
        return 2 * r * asin(sqrt(h))
    }

    /**
     * Fits [bounds] into a `viewW x viewH` box, using the box's real aspect
     * ratio (not padding a square) so an elongated shape (Japan's
     * archipelago) fills most of the box instead of leaving large margins.
     */
    fun project(point: LngLat, bounds: LngLatBounds, viewW: Float, viewH: Float): ViewPoint {
        val w = (bounds.maxLng - bounds.minLng).let { if (it == 0.0) 1.0 else it }
        val h = (bounds.maxLat - bounds.minLat).let { if (it == 0.0) 1.0 else it }
        val scale = min((viewW * 0.94) / w, (viewH * 0.94) / h)
        val offsetX = (viewW - w * scale) / 2
        val offsetY = (viewH - h * scale) / 2
        val x = (point.lng - bounds.minLng) * scale + offsetX
        val y = viewH - ((point.lat - bounds.minLat) * scale + offsetY)
        return ViewPoint(x.toFloat(), y.toFloat())
    }

    /** Inverse of [project] — turns an on-screen tap back into a real lng/lat. */
    fun unproject(p: ViewPoint, bounds: LngLatBounds, viewW: Float, viewH: Float): LngLat {
        val w = (bounds.maxLng - bounds.minLng).let { if (it == 0.0) 1.0 else it }
        val h = (bounds.maxLat - bounds.minLat).let { if (it == 0.0) 1.0 else it }
        val scale = min((viewW * 0.94) / w, (viewH * 0.94) / h)
        val offsetX = (viewW - w * scale) / 2
        val offsetY = (viewH - h * scale) / 2
        val lng = bounds.minLng + (p.x - offsetX) / scale
        val lat = bounds.minLat + (viewH - p.y - offsetY) / scale
        return LngLat(lng, lat)
    }

    /** Which feature (if any) a real lng/lat point falls inside. */
    fun findFeatureIdAt(fc: GeoFeatureCollection, point: LngLat): Int? {
        for (feature in fc.features) {
            if (pointInGeometry(point, feature.geometry)) return feature.properties.id
        }
        return null
    }
}
