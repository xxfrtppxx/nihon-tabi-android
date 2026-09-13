package com.nihontabi.android.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihontabi.android.core.database.PrefectureEntity
import com.nihontabi.android.core.database.VisitEntity
import com.nihontabi.android.core.geo.DotStatus
import com.nihontabi.android.core.geo.LngLatBounds
import com.nihontabi.android.ui.nav.SessionViewModel
import com.nihontabi.android.ui.theme.extendedColors

/**
 * The prefecture boundary data's true bbox stretches to Minami-Torishima
 * (~154E, a speck 1,800km from Tokyo) — cropped to the main islands +
 * Okinawa, the same extent an illustrative "map of Japan" normally shows.
 * Mirrors `COUNTRY_DOT_BOUNDS` in `nihon-tabi-web/app/map/page.tsx`.
 */
private val CountryBounds = LngLatBounds(minLng = 122.7, minLat = 24.0, maxLng = 148.9, maxLat = 45.7)

@Composable
fun MapScreen(
    onOpenPrefecture: (Int) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val prefectures by viewModel.prefectures.collectAsState()
    val visits by viewModel.visits.collectAsState()
    val geojson by viewModel.geojson.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var search by remember { mutableStateOf("") }

    val visitedIds = remember(visits) { visits.filter { it.status == "visited" }.map { it.prefectureId }.toSet() }
    val wantIds = remember(visits) { visits.filter { it.status == "want_to_go" }.map { it.prefectureId }.toSet() }
    val classify: (Int) -> DotStatus = remember(visitedIds, wantIds) {
        { id ->
            when {
                visitedIds.contains(id) && wantIds.contains(id) -> DotStatus.MIXED
                visitedIds.contains(id) -> DotStatus.VISITED
                wantIds.contains(id) -> DotStatus.WANT_TO_GO
                else -> DotStatus.NONE
            }
        }
    }

    val filteredPrefectures = remember(prefectures, search) {
        if (search.isBlank()) emptyList() else prefectures.filter {
            it.nameEn.contains(search, ignoreCase = true) || it.nameJa.contains(search)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            DotMapCanvas(
                geojson = geojson,
                bounds = CountryBounds,
                classify = classify,
                onFeatureClick = onOpenPrefecture,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search prefecture") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (filteredPrefectures.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                        items(filteredPrefectures) { pref ->
                            PrefectureRow(pref, visits) {
                                search = ""
                                onOpenPrefecture(pref.id)
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            tonalElevation = 4.dp,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).padding(top = 72.dp),
        ) {
            Legend()
        }

        IconButton(
            onClick = { sessionViewModel.logout() },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 16.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            tonalElevation = 4.dp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "COVERAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val pct = if (prefectures.isNotEmpty()) {
                    (visitedIds.size * 100f / prefectures.size)
                } else 0f
                Text(
                    "%.1f%%".format(pct),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun PrefectureRow(pref: PrefectureEntity, visits: List<VisitEntity>, onClick: () -> Unit) {
    val visitedCount = remember(visits, pref.id) {
        visits.filter { it.prefectureId == pref.id && it.status == "visited" }
            .map { it.municipalityId }.distinct().size
    }
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row {
            Text(pref.nameEn, modifier = Modifier.weight(1f))
            Text("$visitedCount/${pref.municipalityCount}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun Legend() {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            "LEGEND",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LegendRow(MaterialTheme.colorScheme.primary, "Visited")
        LegendRow(MaterialTheme.extendedColors.plan, "Want to go")
        LegendRow(MaterialTheme.colorScheme.outlineVariant, "Not yet")
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp)),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
