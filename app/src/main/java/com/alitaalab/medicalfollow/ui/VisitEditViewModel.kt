package com.alitaalab.medicalfollow.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alitaalab.medicalfollow.MedicalApp
import com.alitaalab.medicalfollow.data.Medication
import com.alitaalab.medicalfollow.data.Repository
import com.alitaalab.medicalfollow.data.Visit
import com.alitaalab.medicalfollow.util.ImageStorage
import com.alitaalab.medicalfollow.util.parseDecimal
import com.alitaalab.medicalfollow.util.pretty
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MedDraft(
    val key: Long,
    val name: String = "",
    val dose: String = "",
    val frequency: String = "",
    val duration: String = "",
)

/** Holds the add/edit visit form so it survives rotation. */
class VisitEditViewModel(
    private val repo: Repository,
    savedState: SavedStateHandle,
) : ViewModel() {

    val visitId: Long = savedState.get<Long>("id") ?: 0L
    val isNew get() = visitId == 0L

    var loaded by mutableStateOf(isNew)
        private set

    var date by mutableStateOf(LocalDate.now())
    var time by mutableStateOf("")
    var doctorName by mutableStateOf("")
    var specialty by mutableStateOf("")
    var clinic by mutableStateOf("")
    var reason by mutableStateOf("")
    var diagnosis by mutableStateOf("")
    var weight by mutableStateOf("")
    var height by mutableStateOf("")
    var temperature by mutableStateOf("")
    var notes by mutableStateOf("")
    var image by mutableStateOf<String?>(null)
        private set

    val meds = mutableStateListOf<MedDraft>()
    private var nextKey = 1L

    private var originalImage: String? = null
    private val newImages = mutableListOf<String>()
    private var saved = false

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.getVisit(visitId)?.let { item ->
                    val v = item.visit
                    date = v.date
                    time = v.time
                    doctorName = v.doctorName
                    specialty = v.specialty
                    clinic = v.clinic
                    reason = v.reason
                    diagnosis = v.diagnosis
                    weight = v.weightKg?.pretty() ?: ""
                    height = v.heightCm?.pretty() ?: ""
                    temperature = v.temperatureC?.pretty() ?: ""
                    notes = v.notes
                    image = v.prescriptionImage
                    originalImage = v.prescriptionImage
                    item.medications.forEach {
                        meds.add(MedDraft(nextKey++, it.name, it.dose, it.frequency, it.duration))
                    }
                }
                loaded = true
            }
        }
    }

    fun addMed() {
        meds.add(MedDraft(nextKey++))
    }

    fun updateMed(draft: MedDraft) {
        val index = meds.indexOfFirst { it.key == draft.key }
        if (index >= 0) meds[index] = draft
    }

    fun removeMed(draft: MedDraft) {
        meds.removeAll { it.key == draft.key }
    }

    fun setNewImage(path: String) {
        newImages.add(path)
        image = path
    }

    fun removeImage() {
        image = null
    }

    fun save(onSaved: (Long) -> Unit) = viewModelScope.launch {
        val visit = Visit(
            id = visitId,
            date = date,
            time = time.trim(),
            doctorName = doctorName.trim(),
            specialty = specialty.trim(),
            clinic = clinic.trim(),
            reason = reason.trim(),
            diagnosis = diagnosis.trim(),
            weightKg = parseDecimal(weight),
            heightCm = parseDecimal(height),
            temperatureC = parseDecimal(temperature),
            notes = notes.trim(),
            prescriptionImage = image,
        )
        val medications = meds
            .filter { it.name.isNotBlank() }
            .map {
                Medication(
                    visitId = visitId,
                    name = it.name.trim(),
                    dose = it.dose.trim(),
                    frequency = it.frequency.trim(),
                    duration = it.duration.trim(),
                )
            }
        val id = repo.saveVisit(visit, medications)
        saved = true
        // Clean up photos that were replaced or discarded.
        if (originalImage != null && originalImage != image) ImageStorage.delete(originalImage)
        newImages.filter { it != image }.forEach { ImageStorage.delete(it) }
        onSaved(id)
    }

    override fun onCleared() {
        if (!saved) newImages.filter { it != originalImage }.forEach { ImageStorage.delete(it) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MedicalApp
                VisitEditViewModel(app.repository, createSavedStateHandle())
            }
        }
    }
}
