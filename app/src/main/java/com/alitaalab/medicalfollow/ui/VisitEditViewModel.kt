package com.alitaalab.medicalfollow.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.StringRes
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
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.ai.ApiKeyStore
import com.alitaalab.medicalfollow.ai.PrescriptionScanner
import com.alitaalab.medicalfollow.ai.ScanException
import com.alitaalab.medicalfollow.ai.ScannedPrescription
import com.alitaalab.medicalfollow.data.Medication
import com.alitaalab.medicalfollow.data.Repository
import com.alitaalab.medicalfollow.data.Visit
import com.alitaalab.medicalfollow.util.ImageStorage
import com.alitaalab.medicalfollow.util.parseDecimal
import com.alitaalab.medicalfollow.util.pretty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException

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
    private val keyStore: ApiKeyStore,
    savedState: SavedStateHandle,
) : ViewModel() {

    val hasAiKey: Boolean get() = keyStore.key.value.isNotBlank()

    /** True while Claude is reading the prescription photo. */
    var scanning by mutableStateOf(false)
        private set

    /** One-off message for a snackbar after a scan; the screen clears it once shown. */
    var scanMessage by mutableStateOf<Int?>(null)
        private set

    fun consumeScanMessage() {
        scanMessage = null
    }

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

    /** Sets a newly taken/picked photo and, when a key is set, reads it straight away. */
    fun setNewImage(path: String, arabic: Boolean) {
        newImages.add(path)
        image = path
        if (hasAiKey) scanPrescription(arabic)
    }

    fun scanPrescription(arabic: Boolean) {
        val path = image ?: return
        if (scanning || !hasAiKey) return
        scanning = true
        viewModelScope.launch {
            val message = try {
                val result = withContext(Dispatchers.IO) {
                    PrescriptionScanner.read(keyStore.key.value, loadJpeg(path), arabic)
                }
                when {
                    image != path -> null // photo changed meanwhile; ignore the old result
                    !result.isPrescription -> R.string.scan_not_rx
                    else -> {
                        applyScan(result)
                        R.string.scan_done
                    }
                }
            } catch (e: ScanException) {
                scanErrorMessage(e.reason)
            } catch (e: Exception) {
                R.string.scan_err
            }
            scanning = false
            scanMessage = message
        }
    }

    @StringRes
    private fun scanErrorMessage(reason: ScanException.Reason): Int = when (reason) {
        ScanException.Reason.KEY -> R.string.scan_err_key
        ScanException.Reason.OFFLINE -> R.string.scan_err_offline
        ScanException.Reason.REFUSAL -> R.string.scan_err_refusal
        ScanException.Reason.FAILED -> R.string.scan_err
    }

    /** Fills the form from a scan without overwriting anything the parent already typed. */
    private fun applyScan(r: ScannedPrescription) {
        if (diagnosis.isBlank()) diagnosis = r.diagnosis
        if (doctorName.isBlank()) doctorName = r.doctorName
        if (specialty.isBlank()) specialty = r.specialty
        if (clinic.isBlank()) clinic = r.clinic
        if (notes.isBlank()) notes = r.notes
        if (isNew) {
            // A new visit defaults to today; use the date written on the prescription instead.
            try {
                val written = LocalDate.parse(r.visitDate)
                if (!written.isAfter(LocalDate.now())) date = written
            } catch (e: DateTimeParseException) {
                // no readable date on the prescription; keep the form's date
            }
        }
        meds.removeAll { it.name.isBlank() && it.dose.isBlank() && it.frequency.isBlank() && it.duration.isBlank() }
        val existing = meds.map { it.name.trim().lowercase() }.toSet()
        r.medications
            .filter { it.name.isNotBlank() && it.name.trim().lowercase() !in existing }
            .forEach { meds.add(MedDraft(nextKey++, it.name, it.dose, it.frequency, it.duration)) }
    }

    /** Downscales the photo to at most 1600 px and re-encodes it as JPEG for upload. */
    private fun loadJpeg(path: String): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw ScanException(ScanException.Reason.FAILED)
        val scale = minOf(1f, 1600f / maxOf(bitmap.width, bitmap.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }

    fun addMedNamed(name: String) {
        meds.add(MedDraft(nextKey++, name = name))
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
                VisitEditViewModel(app.repository, app.apiKeyStore, createSavedStateHandle())
            }
        }
    }
}
