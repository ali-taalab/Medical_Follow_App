package com.alitaalab.medicalfollow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alitaalab.medicalfollow.MedicalApp
import com.alitaalab.medicalfollow.data.Child
import com.alitaalab.medicalfollow.data.Repository
import com.alitaalab.medicalfollow.data.Vaccine
import com.alitaalab.medicalfollow.data.VisitWithMeds
import com.alitaalab.medicalfollow.util.ImageStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Whether the profile has been loaded from the database yet, and the profile itself. */
data class ChildState(val loaded: Boolean, val child: Child?)

enum class VaccineStatus { TAKEN, OVERDUE, DUE_SOON, UPCOMING }

fun Vaccine.status(today: LocalDate = LocalDate.now()): VaccineStatus = when {
    isTaken -> VaccineStatus.TAKEN
    dueDate.isBefore(today) -> VaccineStatus.OVERDUE
    !dueDate.isAfter(today.plusDays(14)) -> VaccineStatus.DUE_SOON
    else -> VaccineStatus.UPCOMING
}

class AppViewModel(private val repo: Repository) : ViewModel() {

    val childState: StateFlow<ChildState> = repo.child
        .map { ChildState(true, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChildState(false, null))

    val vaccines: StateFlow<List<Vaccine>> = repo.vaccines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visits: StateFlow<List<VisitWithMeds>> = repo.visits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeVisit(id: Long): Flow<VisitWithMeds?> = repo.observeVisit(id)

    fun saveChild(child: Child) = viewModelScope.launch { repo.saveChild(child) }

    fun saveVaccine(vaccine: Vaccine) = viewModelScope.launch { repo.saveVaccine(vaccine) }

    fun deleteVaccine(vaccine: Vaccine) = viewModelScope.launch { repo.deleteVaccine(vaccine) }

    fun addOptionalVaccines(onDone: (Int) -> Unit) = viewModelScope.launch {
        val birth = childState.value.child?.birthDate ?: return@launch
        onDone(repo.addOptionalVaccines(birth))
    }

    fun deleteVisit(item: VisitWithMeds) = viewModelScope.launch {
        repo.deleteVisit(item.visit)
        ImageStorage.delete(item.visit.prescriptionImage)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MedicalApp
                AppViewModel(app.repository)
            }
        }
    }
}
