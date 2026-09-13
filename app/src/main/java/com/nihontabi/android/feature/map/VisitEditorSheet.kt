@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nihontabi.android.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihontabi.android.core.database.MunicipalityEntity
import com.nihontabi.android.core.database.PrefectureEntity
import com.nihontabi.android.core.database.VisitEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val STATUS_VISITED = "visited"
private const val STATUS_WANT = "want_to_go"

private sealed interface EditorMode {
    data object List : EditorMode
    data class Form(val existing: VisitEntity?) : EditorMode
}

@Composable
fun VisitEditorSheet(
    municipalityId: Int,
    onDismiss: () -> Unit,
    viewModel: VisitEditorViewModel = hiltViewModel(),
) {
    val visits by viewModel.visitsFor(municipalityId).collectAsState(initial = emptyList())
    var municipality by remember { mutableStateOf<MunicipalityEntity?>(null) }
    var prefecture by remember { mutableStateOf<PrefectureEntity?>(null) }
    var mode by remember { mutableStateOf<EditorMode>(EditorMode.List) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(municipalityId) {
        municipality = viewModel.getMunicipality(municipalityId)
        prefecture = municipality?.let { viewModel.getPrefecture(it.prefectureId) }
    }

    // Opens straight into the entry form when this city has no records yet,
    // same as `VisitEditor`'s `mode="new"` in nihon-tabi-web.
    LaunchedEffect(visits, initialized) {
        if (!initialized) {
            mode = if (visits.isEmpty()) EditorMode.Form(null) else EditorMode.List
            initialized = true
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            municipality?.let {
                Text(it.nameEn, style = MaterialTheme.typography.titleMedium)
            }

            when (val m = mode) {
                is EditorMode.List -> VisitRecordsList(
                    visits = visits,
                    onEdit = { mode = EditorMode.Form(it) },
                    onAddNew = { mode = EditorMode.Form(null) },
                    onClose = onDismiss,
                )
                is EditorMode.Form -> {
                    val muni = municipality
                    val pref = prefecture
                    if (muni != null && pref != null) {
                        VisitEntryForm(
                            municipality = muni,
                            prefecture = pref,
                            existing = m.existing,
                            showBack = visits.isNotEmpty(),
                            onBack = { mode = EditorMode.List },
                            onClose = onDismiss,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitRecordsList(
    visits: List<VisitEntity>,
    onEdit: (VisitEntity) -> Unit,
    onAddNew: () -> Unit,
    onClose: () -> Unit,
) {
    Column {
        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(visits, key = { it.localId }) { visit ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            if (visit.status == STATUS_VISITED) "Visited" else "Want to go",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        visit.visitedOn?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                    if (visit.rating != null && visit.rating > 0) {
                        Text("★".repeat(visit.rating), color = MaterialTheme.colorScheme.primary)
                    }
                    visit.note?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { onEdit(visit) }) { Text("Edit") }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddNew, modifier = Modifier.weight(1f)) { Text("+ Add new record") }
            OutlinedButton(onClick = onClose) { Text("Close") }
        }
    }
}

@Composable
private fun VisitEntryForm(
    municipality: MunicipalityEntity,
    prefecture: PrefectureEntity,
    existing: VisitEntity?,
    showBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    viewModel: VisitEditorViewModel,
) {
    var status by remember { mutableStateOf(existing?.status ?: STATUS_VISITED) }
    var visitedOn by remember { mutableStateOf(existing?.visitedOn ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var rating by remember { mutableStateOf(existing?.rating ?: 0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun backOrClose() {
        if (showBack) onBack() else onClose()
    }

    Column {
        if (showBack) {
            TextButton(onClick = onBack) { Text("← Back to records") }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            SegmentedButton(
                selected = status == STATUS_VISITED,
                onClick = {
                    status = STATUS_VISITED
                    if (visitedOn.isNotBlank() && visitedOn < MIN_VISITED_DATE) visitedOn = ""
                },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text("Visited") }
            SegmentedButton(
                selected = status == STATUS_WANT,
                onClick = {
                    status = STATUS_WANT
                    if (visitedOn.isNotBlank() && visitedOn < todayStr()) visitedOn = ""
                },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text("Want to go") }
        }

        OutlinedTextField(
            value = visitedOn,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            placeholder = { Text("yyyy-mm-dd") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            trailingIcon = {
                TextButton(onClick = { showDatePicker = true }) { Text("Pick") }
            },
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        Row(modifier = Modifier.padding(top = 12.dp)) {
            for (n in 1..5) {
                IconButton(onClick = { rating = if (rating == n) 0 else n }) {
                    Icon(
                        imageVector = if (n <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.save(
                        localId = existing?.localId,
                        municipality = municipality,
                        prefecture = prefecture,
                        status = status,
                        visitedOn = visitedOn.ifBlank { null },
                        note = note.ifBlank { null },
                        rating = rating.takeIf { it > 0 },
                        onDone = { backOrClose() },
                    )
                },
                modifier = Modifier.weight(1f),
            ) { Text("Save") }

            if (existing != null) {
                OutlinedButton(onClick = { showDeleteConfirm = true }) { Text("Delete") }
            }
            OutlinedButton(onClick = onClose) { Text("Close") }
        }
    }

    if (showDatePicker) {
        val initialMillis = runCatching {
            LocalDate.parse(visitedOn.ifBlank { todayStr() }).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        visitedOn = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this record?") },
            text = { Text("This can't be undone.", textAlign = TextAlign.Start) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(existing.localId) { backOrClose() }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

private const val MIN_VISITED_DATE = "1990-01-01"

private fun todayStr(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
