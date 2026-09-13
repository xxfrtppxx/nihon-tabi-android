package com.nihontabi.android.feature.visits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihontabi.android.core.database.VisitEntity
import com.nihontabi.android.core.database.VisitsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class VisitsListViewModel @Inject constructor(
    visitsRepository: VisitsRepository,
) : ViewModel() {
    val visits: StateFlow<List<VisitEntity>> = visitsRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
