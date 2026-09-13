package com.nihontabi.android.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihontabi.android.core.database.MunicipalityEntity
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrefectureViewModel @Inject constructor(
    private val geoRepository: GeoRepository,
    visitsRepository: VisitsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val prefectureId: Int = checkNotNull(savedStateHandle["prefectureId"])

    private val _prefecture = MutableStateFlow<PrefectureEntity?>(null)
    val prefecture: StateFlow<PrefectureEntity?> = _prefecture.asStateFlow()

    val municipalities: StateFlow<List<MunicipalityEntity>> = geoRepository
        .observeMunicipalities(prefectureId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visits: StateFlow<List<VisitEntity>> = visitsRepository.observeAll()
        .map { list -> list.filter { it.prefectureId == prefectureId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _geojson = MutableStateFlow<GeoFeatureCollection?>(null)
    val geojson: StateFlow<GeoFeatureCollection?> = _geojson.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            _prefecture.value = geoRepository.getPrefecture(prefectureId)
            geoRepository.ensureMunicipalitiesLoaded(prefectureId)
            _geojson.value = geoRepository.getMunicipalitiesGeoJson(prefectureId)
            _loading.value = false
        }
    }
}
