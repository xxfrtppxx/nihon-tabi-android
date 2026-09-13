package com.nihontabi.android.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihontabi.android.core.database.PrefectureEntity
import com.nihontabi.android.core.database.VisitEntity
import com.nihontabi.android.core.database.VisitsRepository
import com.nihontabi.android.core.geo.GeoFeatureCollection
import com.nihontabi.android.core.geo.GeoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val geoRepository: GeoRepository,
    visitsRepository: VisitsRepository,
) : ViewModel() {

    val prefectures: StateFlow<List<PrefectureEntity>> = geoRepository.observePrefectures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visits: StateFlow<List<VisitEntity>> = visitsRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _geojson = MutableStateFlow<GeoFeatureCollection?>(null)
    val geojson: StateFlow<GeoFeatureCollection?> = _geojson.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            geoRepository.ensurePrefecturesLoaded()
            _geojson.value = geoRepository.getPrefecturesGeoJson()
            _loading.value = false
        }
    }
}
