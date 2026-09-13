package com.nihontabi.android.feature.visits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihontabi.android.core.database.VisitEntity

private enum class VisitFilter(val label: String) {
    ALL("All"), VISITED("Visited"), WANT("Want to go")
}

private data class YearGroup(val year: String, val visits: List<VisitEntity>)

@Composable
fun VisitsListScreen(
    onOpenPrefecture: (Int) -> Unit = {},
    viewModel: VisitsListViewModel = hiltViewModel(),
) {
    val visits by viewModel.visits.collectAsState()
    var filter by remember { mutableStateOf(VisitFilter.ALL) }
    var newestFirst by remember { mutableStateOf(true) }

    val groups = remember(visits, filter, newestFirst) {
        val filtered = visits.filter {
            when (filter) {
                VisitFilter.ALL -> true
                VisitFilter.VISITED -> it.status == "visited"
                VisitFilter.WANT -> it.status == "want_to_go"
            }
        }
        val dated = filtered.filter { !it.visitedOn.isNullOrBlank() }
            .sortedBy { it.visitedOn }
            .let { if (newestFirst) it.reversed() else it }
        val undated = filtered.filter { it.visitedOn.isNullOrBlank() }

        val byYear = LinkedHashMap<String, MutableList<VisitEntity>>()
        for (v in dated) {
            val year = v.visitedOn!!.take(4)
            byYear.getOrPut(year) { mutableListOf() }.add(v)
        }
        val years = byYear.keys.sortedWith(if (newestFirst) compareByDescending { it } else compareBy { it })
        val result = years.map { YearGroup(it, byYear.getValue(it)) }.toMutableList()
        if (undated.isNotEmpty()) result.add(YearGroup("Undated", undated))
        result
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VisitFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            TextButton(onClick = { newestFirst = !newestFirst }) {
                Text(if (newestFirst) "Sort · Newest" else "Sort · Oldest")
            }
        }

        if (visits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No visits yet — head to the Atlas and pick a city to log one.")
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            groups.forEach { group ->
                item(key = "header-${group.year}") {
                    Text(
                        group.year,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                    )
                }
                items(group.visits, key = { it.localId }) { visit ->
                    VisitRow(visit, onClick = { onOpenPrefecture(visit.prefectureId) })
                }
            }
        }
    }
}

@Composable
private fun VisitRow(visit: VisitEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(visit.municipalityNameEn, style = MaterialTheme.typography.titleMedium)
                visit.visitedOn?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            Text(
                "${visit.prefectureNameEn} · ${visit.region}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (visit.status == "visited") "VISITED" else "WANT TO GO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (visit.rating != null && visit.rating > 0) {
                    Text("★".repeat(visit.rating), color = MaterialTheme.colorScheme.primary)
                }
            }
            visit.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
