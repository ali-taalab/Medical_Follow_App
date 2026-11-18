package com.alitaalab.medicalfollow.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.data.Medicines
import com.alitaalab.medicalfollow.ui.MedDraft
import com.alitaalab.medicalfollow.ui.VisitEditViewModel
import com.alitaalab.medicalfollow.ui.components.DateField
import com.alitaalab.medicalfollow.ui.components.FormField
import com.alitaalab.medicalfollow.ui.components.SectionTitle
import com.alitaalab.medicalfollow.util.ImageStorage
import kotlinx.coroutines.launch
import java.io.File

/** Which medicine row the picker fills: a row's key, or [NEW_ROW] to add a new one. */
private const val NEW_ROW = -1L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitEditScreen(
    onBack: () -> Unit,
    onSaved: (id: Long, isNew: Boolean) -> Unit,
    vm: VisitEditViewModel = viewModel(factory = VisitEditViewModel.Factory),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    // The UI language follows the device: Arabic resources unless the phone is in English.
    val arabic = !LocalConfiguration.current.locales[0].language.startsWith("en")
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var pickerFor by rememberSaveable { mutableStateOf<Long?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingCameraPath
        if (path != null) {
            if (ok) vm.setNewImage(path, arabic) else ImageStorage.delete(path)
        }
        pendingCameraPath = null
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch { ImageStorage.copyToInternal(context, uri)?.let { vm.setNewImage(it, arabic) } }
        }
    }

    val message = vm.scanMessage
    LaunchedEffect(message) {
        if (message != null) {
            vm.consumeScanMessage()
            snackbar.showSnackbar(context.getString(message))
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
        snackbarHost = { SnackbarHost(snackbar) },
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

            // Photo first: reading it can fill in the diagnosis and medicines below.
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
            if (image != null) {
                ScanSection(
                    hasKey = vm.hasAiKey,
                    scanning = vm.scanning,
                    onScan = { vm.scanPrescription(arabic) },
                )
            }

            SectionTitle(stringResource(R.string.prescription))
            vm.meds.forEach { med ->
                key(med.key) {
                    MedicationEditor(
                        med,
                        onChange = vm::updateMed,
                        onRemove = { vm.removeMed(med) },
                        onPick = { pickerFor = med.key },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::addMed, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_medication))
                }
                OutlinedButton(onClick = { pickerFor = NEW_ROW }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_from_list))
                }
            }

            FormField(vm.notes, { vm.notes = it }, stringResource(R.string.notes), singleLine = false, minLines = 3)
            Spacer(Modifier.height(24.dp))
        }
    }

    pickerFor?.let { target ->
        MedicinePickerDialog(
            arabic = arabic,
            onDismiss = { pickerFor = null },
            onPick = { name ->
                val row = vm.meds.firstOrNull { it.key == target }
                if (row != null) vm.updateMed(row.copy(name = name)) else vm.addMedNamed(name)
                pickerFor = null
            },
        )
    }
}

@Composable
private fun ScanSection(hasKey: Boolean, scanning: Boolean, onScan: () -> Unit) {
    when {
        !hasKey -> Text(
            stringResource(R.string.scan_need_key),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        scanning -> Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.scanning), fontWeight = FontWeight.SemiBold)
            }
        }
        else -> OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.scan_btn))
        }
    }
}

@Composable
private fun MedicationEditor(med: MedDraft, onChange: (MedDraft) -> Unit, onRemove: () -> Unit, onPick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FormField(med.name, { onChange(med.copy(name = it)) }, stringResource(R.string.medicine_name), Modifier.weight(1f))
                IconButton(onClick = onPick) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.choose_from_list))
                }
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

/** Searchable list of common children's medicines; tapping a generic or brand name returns it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicinePickerDialog(arabic: Boolean, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val groups = Medicines.groups
        .map { g -> g to g.items.filter { it.matches(query) } }
        .filter { it.second.isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_from_list)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_medicine)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.med_list_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (groups.isEmpty()) {
                    Text(stringResource(R.string.no_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    groups.forEach { (group, items) ->
                        item(key = "g_${group.en}") { SectionTitle(group.title(arabic)) }
                        items(items, key = { it.en }) { m ->
                            val generic = m.generic(arabic)
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onPick(generic) }
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                ) {
                                    Text(generic, fontWeight = FontWeight.SemiBold)
                                    if (arabic) {
                                        Text(m.en, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (m.brands.isNotEmpty()) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        m.brands.forEach { brand ->
                                            AssistChip(onClick = { onPick("$brand ($generic)") }, label = { Text(brand) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
