package com.nihontabi.android.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihontabi.android.core.geo.DotStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrefectureScreen(
    prefectureId: Int,
    onBack: () -> Unit,
    viewModel: PrefectureViewModel = hiltViewModel(),
) {
    val prefecture by viewModel.prefecture.collectAsState()
    val municipalities by viewModel.municipalities.collectAsState()
    val visits by viewModel.visits.collectAsState()
    val geojson by viewModel.geojson.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var selectedMunicipalityId by remember { mutableStateOf<Int?>(null) }

    val visitedIds = remember(visits) { visits.filter { it.status == "visited" }.map { it.municipalityId }.toSet() }
    val wantIds = remember(visits) { visits.filter { it.status == "want_to_go" }.map { it.municipalityId }.toSet() }
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(prefecture?.nameEn ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                DotMapCanvas(
                    geojson = geojson,
                    bounds = null,
                    classify = classify,
                    onFeatureClick = { id -> selectedMunicipalityId = id },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    selectedMunicipalityId?.let { id ->
        VisitEditorSheet(municipalityId = id, onDismiss = { selectedMunicipalityId = null })
    }
}
