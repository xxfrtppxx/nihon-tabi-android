package com.nihontabi.android.core.geo

/** Mirrors `DotStatus` in `nihon-tabi-web/components/dot-map.tsx`. */
enum class DotStatus { VISITED, WANT_TO_GO, MIXED, NONE }

data class Dot(val x: Float, val y: Float, val status: DotStatus)

data class DotGridResult(
    val dots: List<Dot>,
    val bounds: LngLatBounds?,
    /** One polyline per ring of every feature — the coastline/border wash under the dots. */
    val boundaryPolylines: List<List<ViewPoint>>,
    val cellSize: Float,
)

/**
 * Port of `useDotGrid` from `nihon-tabi-web/components/dot-map.tsx`: samples
 * a `cols x rows` grid of lng/lat points, keeps only the ones that fall
 * inside some feature's real geometry (so the dots trace the actual
 * coastline/border), and classifies each via [classify].
 */
object DotGrid {

    fun compute(
        geojson: GeoFeatureCollection?,
        boundsOverride: LngLatBounds?,
        classify: (Int) -> DotStatus,
        cols: Int,
        rows: Int,
        viewW: Float,
        viewH: Float,
    ): DotGridResult {
        val bounds = boundsOverride ?: geojson?.let { GeoMath.boundsOfFeatureCollection(it) }
        val cellSize = minOf(viewW / cols, viewH / rows)

        if (geojson == null || geojson.features.isEmpty() || bounds == null) {
            return DotGridResult(emptyList(), bounds, emptyList(), cellSize)
        }

        data class IndexedFeature(val id: Int?, val geometry: GeoGeometry, val bbox: LngLatBounds)

        val features = geojson.features.mapNotNull { f ->
            val bbox = GeoMath.boundsOfGeometry(f.geometry) ?: return@mapNotNull null
            IndexedFeature(f.properties.id, f.geometry, bbox)
        }

        val dots = ArrayList<Dot>(cols * rows)
        for (row in 0 until rows) {
            val lat = bounds.minLat + ((row + 0.5) / rows) * (bounds.maxLat - bounds.minLat)
            for (col in 0 until cols) {
                val lng = bounds.minLng + ((col + 0.5) / cols) * (bounds.maxLng - bounds.minLng)

                var status: DotStatus? = null
                for (f in features) {
                    if (lng < f.bbox.minLng || lng > f.bbox.maxLng || lat < f.bbox.minLat || lat > f.bbox.maxLat) continue
                    if (!GeoMath.pointInGeometry(LngLat(lng, lat), f.geometry)) continue
                    status = if (f.id != null) classify(f.id) else DotStatus.NONE
                    break
                }
                if (status == null) continue
                val p = GeoMath.project(LngLat(lng, lat), bounds, viewW, viewH)
                dots.add(Dot(p.x, p.y, status))
            }
        }

        val projectFn = { point: LngLat -> GeoMath.project(point, bounds, viewW, viewH) }
        val boundary = geojson.features.flatMap { geometryToPolylines(it.geometry, projectFn) }

        return DotGridResult(dots, bounds, boundary, cellSize)
    }

    /** A single feature's real outline, projected into view space — one polyline per ring. */
    fun geometryToPolylines(geometry: GeoGeometry, project: (LngLat) -> ViewPoint): List<List<ViewPoint>> =
        geometry.polygons.flatMap { polygon ->
            polygon.map { ring -> ring.map { pt -> project(LngLat(pt[0], pt[1])) } }
        }
}
