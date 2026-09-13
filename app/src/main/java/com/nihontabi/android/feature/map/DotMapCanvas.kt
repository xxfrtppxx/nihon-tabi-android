package com.nihontabi.android.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.nihontabi.android.core.geo.Dot
import com.nihontabi.android.core.geo.DotGrid
import com.nihontabi.android.core.geo.DotStatus
import com.nihontabi.android.core.geo.GeoFeatureCollection
import com.nihontabi.android.core.geo.GeoMath
import com.nihontabi.android.core.geo.LngLatBounds
import com.nihontabi.android.core.geo.ViewPoint
import com.nihontabi.android.ui.theme.extendedColors
import kotlin.math.max

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 4f
private const val TAP_SLOP_PX = 12f

/**
 * Port of `DotMapFill`/`DotMapSvg`/`useDotGrid` from
 * `nihon-tabi-web/components/dot-map.tsx`, rendered with a Compose [Canvas]
 * instead of SVG. Renders [geojson] as a dot-matrix (filled = visited,
 * outlined = want-to-go, dim = none) traced over the real coastline/border,
 * pinch-zoomable (1x-4x, matching the web's `MIN_ZOOM`/`MAX_ZOOM`) and
 * tap-to-select ([onFeatureClick]).
 */
@Composable
fun DotMapCanvas(
    geojson: GeoFeatureCollection?,
    bounds: LngLatBounds?,
    classify: (Int) -> DotStatus,
    modifier: Modifier = Modifier,
    cols: Int = 44,
    rows: Int = 44,
    onFeatureClick: ((Int) -> Unit)? = null,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val effectiveBounds = bounds ?: geojson?.let { GeoMath.boundsOfFeatureCollection(it) }

    val grid = remember(geojson, effectiveBounds, canvasSize) {
        if (geojson == null || effectiveBounds == null || canvasSize.width == 0 || canvasSize.height == 0) {
            null
        } else {
            DotGrid.compute(
                geojson = geojson,
                boundsOverride = effectiveBounds,
                classify = classify,
                cols = cols,
                rows = rows,
                viewW = canvasSize.width.toFloat(),
                viewH = canvasSize.height.toFloat(),
            )
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val planColor = MaterialTheme.extendedColors.plan
    val dotDimColor = MaterialTheme.extendedColors.dotDim
    val dotCoastColor = MaterialTheme.extendedColors.dotCoast

    fun clampOffset(candidate: Offset, currentScale: Float, size: IntSize): Offset {
        val maxX = (size.width * (currentScale - 1f)) / 2f
        val maxY = (size.height * (currentScale - 1f)) / 2f
        return Offset(
            candidate.x.coerceIn(-maxX, maxX),
            candidate.y.coerceIn(-maxY, maxY),
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(geojson, boundsKey(effectiveBounds)) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var moved = false
                    var totalMovement = 0f
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                scale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                offset = clampOffset(offset + panChange, scale, canvasSize)
                                moved = true
                                event.changes.forEach { it.consume() }
                            }
                        } else if (pressedCount == 1) {
                            val change = event.changes.first()
                            val delta = change.positionChange()
                            totalMovement += delta.getDistance()
                            if (scale > MIN_ZOOM && totalMovement > TAP_SLOP_PX) {
                                offset = clampOffset(offset + delta, scale, canvasSize)
                                moved = true
                                change.consume()
                            } else if (totalMovement > TAP_SLOP_PX) {
                                moved = true
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (!moved && onFeatureClick != null && geojson != null && effectiveBounds != null && canvasSize.width > 0) {
                        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                        val unscaled = center + (down.position - offset - center) / scale
                        val point = GeoMath.unproject(
                            ViewPoint(unscaled.x, unscaled.y),
                            effectiveBounds,
                            canvasSize.width.toFloat(),
                            canvasSize.height.toFloat(),
                        )
                        GeoMath.findFeatureIdAt(geojson, point)?.let(onFeatureClick)
                    }
                }
            },
    ) {
        if (grid == null) return@Canvas
        withTransform({
            translate(offset.x, offset.y)
            scale(scale, scale, pivot = center)
        }) {
            grid.boundaryPolylines.forEach { polyline ->
                if (polyline.size < 2) return@forEach
                val path = Path().apply {
                    moveTo(polyline[0].x, polyline[0].y)
                    for (i in 1 until polyline.size) lineTo(polyline[i].x, polyline[i].y)
                    close()
                }
                drawPath(path, color = dotCoastColor, style = Stroke(width = 1f))
            }

            val bigDot = grid.cellSize * 0.58f
            val dimDot = grid.cellSize * 0.2f
            grid.dots.forEach { dot ->
                drawDot(dot, bigDot, dimDot, accentColor, planColor, dotDimColor)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDot(
    dot: Dot,
    bigDot: Float,
    dimDot: Float,
    accentColor: androidx.compose.ui.graphics.Color,
    planColor: androidx.compose.ui.graphics.Color,
    dimColor: androidx.compose.ui.graphics.Color,
) {
    when (dot.status) {
        DotStatus.VISITED, DotStatus.MIXED -> drawRoundRect(
            color = accentColor,
            topLeft = Offset(dot.x - bigDot / 2, dot.y - bigDot / 2),
            size = Size(bigDot, bigDot),
            cornerRadius = CornerRadius(bigDot * 0.22f, bigDot * 0.22f),
        )
        DotStatus.WANT_TO_GO -> drawRoundRect(
            color = planColor,
            topLeft = Offset(dot.x - bigDot / 2, dot.y - bigDot / 2),
            size = Size(bigDot, bigDot),
            cornerRadius = CornerRadius(bigDot * 0.22f, bigDot * 0.22f),
            style = Stroke(width = max(0.5f, bigDot * 0.12f)),
        )
        DotStatus.NONE -> drawRoundRect(
            color = dimColor,
            topLeft = Offset(dot.x - dimDot / 2, dot.y - dimDot / 2),
            size = Size(dimDot, dimDot),
            cornerRadius = CornerRadius(dimDot * 0.25f, dimDot * 0.25f),
        )
    }
}

/** Bounds isn't stable/equatable enough on its own to key `pointerInput` cheaply; rounds to avoid churn from float noise. */
private fun boundsKey(bounds: LngLatBounds?): String =
    bounds?.let { "${it.minLng},${it.minLat},${it.maxLng},${it.maxLat}" } ?: "none"
