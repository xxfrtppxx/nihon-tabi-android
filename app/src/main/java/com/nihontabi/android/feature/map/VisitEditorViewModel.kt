package com.nihontabi.android.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihontabi.android.core.database.MunicipalityEntity
import com.nihontabi.android.core.database.PrefectureEntity
import com.nihontabi.android.core.database.VisitEntity
import com.nihontabi.android.core.database.VisitsRepository
import com.nihontabi.android.core.geo.GeoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VisitEditorViewModel @Inject constructor(
    private val geoRepository: GeoRepository,
    private val visitsRepository: VisitsRepository,
) : ViewModel() {

    fun visitsFor(municipalityId: Int): Flow<List<VisitEntity>> =
        visitsRepository.observeForMunicipality(municipalityId)

    suspend fun getMunicipality(id: Int): MunicipalityEntity? = geoRepository.getMunicipality(id)

    suspend fun getPrefecture(id: Int): PrefectureEntity? = geoRepository.getPrefecture(id)

    fun save(
        localId: String?,
        municipality: MunicipalityEntity,
        prefecture: PrefectureEntity,
        status: String,
        visitedOn: String?,
        note: String?,
        rating: Int?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            visitsRepository.save(localId, municipality, prefecture, status, visitedOn, note, rating)
            onDone()
        }
    }

    fun delete(localId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            visitsRepository.delete(localId)
            onDone()
        }
    }
}
