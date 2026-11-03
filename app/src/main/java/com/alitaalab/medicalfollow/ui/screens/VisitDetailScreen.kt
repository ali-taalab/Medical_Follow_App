package com.alitaalab.medicalfollow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.data.Medication
import com.alitaalab.medicalfollow.ui.AppViewModel
import com.alitaalab.medicalfollow.ui.components.LabeledValue
import com.alitaalab.medicalfollow.ui.components.SectionTitle
import com.alitaalab.medicalfollow.util.ReportBuilder
import com.alitaalab.medicalfollow.util.formatted
import com.alitaalab.medicalfollow.util.pretty
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(vm: AppViewModel, visitId: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val flow = remember(visitId) { vm.observeVisit(visitId) }
    val item by flow.collectAsState(initial = null)
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var showImage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.visit_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    item?.let { current ->
                        IconButton(onClick = { ReportBuilder.share(context, ReportBuilder.visitText(context, current)) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val current = item ?: return@Scaffold
        val v = current.visit
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                v.date.formatted() + if (v.time.isNotBlank()) " • ${v.time}" else "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    LabeledValue(stringResource(R.string.doctor), v.doctorName)
                    LabeledValue(stringResource(R.string.specialty), v.specialty)
                    LabeledValue(stringResource(R.string.clinic), v.clinic)
                    LabeledValue(stringResource(R.string.reason), v.reason)
                    LabeledValue(stringResource(R.string.diagnosis), v.diagnosis)
                    LabeledValue(stringResource(R.string.weight_kg), v.weightKg?.pretty())
                    LabeledValue(stringResource(R.string.height_cm), v.heightCm?.pretty())
                    LabeledValue(stringResource(R.string.temperature_c), v.temperatureC?.pretty())
                }
            }

            SectionTitle(stringResource(R.string.prescription))
            if (current.medications.isEmpty()) {
                Text(stringResource(R.string.no_medications), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                current.medications.forEach { MedicationRow(it) }
            }

            if (v.prescriptionImage != null) {
                SectionTitle(stringResource(R.string.prescription_photo))
                AsyncImage(
                    model = File(v.prescriptionImage),
                    contentDescription = stringResource(R.string.prescription_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showImage = true },
                )
            }

            if (v.notes.isNotBlank()) {
                SectionTitle(stringResource(R.string.notes))
                Text(v.notes, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }

        if (showImage && v.prescriptionImage != null) {
            Dialog(
                onDismissRequest = { showImage = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    AsyncImage(
                        model = File(v.prescriptionImage),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    IconButton(onClick = { showImage = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                    }
                }
            }
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.delete)) },
                text = { Text(stringResource(R.string.confirm_delete_visit)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        vm.deleteVisit(current)
                        onBack()
                    }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}

@Composable
private fun MedicationRow(m: Medication) {
    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(m.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val details = listOf(
                    m.dose.takeIf { it.isNotBlank() }?.let { "${stringResource(R.string.dose)}: $it" },
                    m.frequency.takeIf { it.isNotBlank() }?.let { "${stringResource(R.string.frequency)}: $it" },
                    m.duration.takeIf { it.isNotBlank() }?.let { "${stringResource(R.string.duration)}: $it" },
                ).filterNotNull()
                details.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
