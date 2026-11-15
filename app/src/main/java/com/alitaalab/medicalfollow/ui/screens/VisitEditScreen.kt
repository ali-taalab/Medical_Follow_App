package com.alitaalab.medicalfollow.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.ui.MedDraft
import com.alitaalab.medicalfollow.ui.VisitEditViewModel
import com.alitaalab.medicalfollow.ui.components.DateField
import com.alitaalab.medicalfollow.ui.components.FormField
import com.alitaalab.medicalfollow.ui.components.SectionTitle
import com.alitaalab.medicalfollow.util.ImageStorage
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitEditScreen(
    onBack: () -> Unit,
    onSaved: (id: Long, isNew: Boolean) -> Unit,
    vm: VisitEditViewModel = viewModel(factory = VisitEditViewModel.Factory),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by rememberSaveable { mutableStateOf(false) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingCameraPath
        if (path != null) {
            if (ok) vm.setNewImage(path) else ImageStorage.delete(path)
        }
        pendingCameraPath = null
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch { ImageStorage.copyToInternal(context, uri)?.let { vm.setNewImage(it) } }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (vm.isNew) R.string.add_visit else R.string.edit_visit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        enabled = vm.loaded && !saving,
                        onClick = {
                            saving = true
                            val isNew = vm.isNew
                            vm.save { id -> onSaved(id, isNew) }
                        },
                    ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { padding ->
        if (!vm.loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    label = stringResource(R.string.visit_date),
                    date = vm.date,
                    onDateChange = { it?.let { d -> vm.date = d } },
                    modifier = Modifier.weight(1.4f),
                )
                FormField(vm.time, { vm.time = it }, stringResource(R.string.time_hint), modifier = Modifier.weight(1f))
            }
            FormField(vm.doctorName, { vm.doctorName = it }, stringResource(R.string.doctor_name))
            FormField(vm.specialty, { vm.specialty = it }, stringResource(R.string.specialty))
            FormField(vm.clinic, { vm.clinic = it }, stringResource(R.string.clinic))
            FormField(vm.reason, { vm.reason = it }, stringResource(R.string.reason), singleLine = false)
            FormField(vm.diagnosis, { vm.diagnosis = it }, stringResource(R.string.diagnosis), singleLine = false)

            SectionTitle(stringResource(R.string.measurements))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(vm.weight, { vm.weight = it }, stringResource(R.string.weight_kg), Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                FormField(vm.height, { vm.height = it }, stringResource(R.string.height_cm), Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                FormField(vm.temperature, { vm.temperature = it }, stringResource(R.string.temperature_c), Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            }

            SectionTitle(stringResource(R.string.prescription))
            vm.meds.forEach { med ->
                key(med.key) {
                    MedicationEditor(med, onChange = vm::updateMed, onRemove = { vm.removeMed(med) })
                }
            }
            OutlinedButton(onClick = vm::addMed, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_medication))
            }

            SectionTitle(stringResource(R.string.prescription_photo))
            val image = vm.image
            if (image != null) {
                Box {
                    AsyncImage(
                        model = File(image),
                        contentDescription = stringResource(R.string.prescription_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    FilledTonalIconButton(
                        onClick = vm::removeImage,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    ) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_photo)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val file = ImageStorage.newImageFile(context)
                        pendingCameraPath = file.absolutePath
                        try {
                            takePicture.launch(ImageStorage.uriForFile(context, file))
                        } catch (e: ActivityNotFoundException) {
                            pendingCameraPath = null
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.take_photo))
                }
                OutlinedButton(
                    onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.choose_photo))
                }
            }

            FormField(vm.notes, { vm.notes = it }, stringResource(R.string.notes), singleLine = false, minLines = 3)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MedicationEditor(med: MedDraft, onChange: (MedDraft) -> Unit, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FormField(med.name, { onChange(med.copy(name = it)) }, stringResource(R.string.medicine_name), Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
            FormField(med.dose, { onChange(med.copy(dose = it)) }, stringResource(R.string.dose_hint))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(med.frequency, { onChange(med.copy(frequency = it)) }, stringResource(R.string.frequency_hint), Modifier.weight(1f))
                FormField(med.duration, { onChange(med.copy(duration = it)) }, stringResource(R.string.duration_hint), Modifier.weight(1f))
            }
        }
    }
}
