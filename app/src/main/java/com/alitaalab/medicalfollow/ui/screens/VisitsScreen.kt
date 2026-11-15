package com.alitaalab.medicalfollow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.data.VisitWithMeds
import com.alitaalab.medicalfollow.ui.AppViewModel
import com.alitaalab.medicalfollow.ui.components.EmptyState
import com.alitaalab.medicalfollow.ui.components.SectionTitle
import com.alitaalab.medicalfollow.util.formatted
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitsScreen(vm: AppViewModel, onOpenVisit: (Long) -> Unit, onAddVisit: () -> Unit) {
    val visits by vm.visits.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val upcoming = visits.filter { it.visit.date.isAfter(today) }.sortedBy { it.visit.date }
    val past = visits.filter { !it.visit.date.isAfter(today) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_visits)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddVisit,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_visit)) },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (visits.isEmpty()) {
            EmptyState(Icons.Default.MedicalServices, stringResource(R.string.no_visits), Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (upcoming.isNotEmpty()) {
                    item(key = "h_up") { SectionTitle(stringResource(R.string.upcoming_visits)) }
                    items(upcoming, key = { it.visit.id }) { VisitCard(it, upcoming = true) { onOpenVisit(it.visit.id) } }
                }
                if (past.isNotEmpty()) {
                    item(key = "h_past") { SectionTitle(stringResource(R.string.past_visits)) }
                    items(past, key = { it.visit.id }) { VisitCard(it, upcoming = false) { onOpenVisit(it.visit.id) } }
                }
            }
        }
    }
}

@Composable
private fun VisitCard(item: VisitWithMeds, upcoming: Boolean, onClick: () -> Unit) {
    val v = item.visit
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = if (upcoming) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    v.date.formatted() + if (v.time.isNotBlank()) " • ${v.time}" else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (v.prescriptionImage != null) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                if (item.medications.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" ${item.medications.size}", style = MaterialTheme.typography.labelMedium)
                }
            }
            val doctor = listOf(v.doctorName, v.specialty).filter { it.isNotBlank() }.joinToString(" - ")
            if (doctor.isNotBlank()) {
                Text(doctor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (v.reason.isNotBlank()) {
                Text("${stringResource(R.string.reason)}: ${v.reason}", style = MaterialTheme.typography.bodyMedium)
            }
            if (v.diagnosis.isNotBlank()) {
                Text("${stringResource(R.string.diagnosis)}: ${v.diagnosis}", style = MaterialTheme.typography.bodyMedium)
            }
            if (item.medications.isNotEmpty()) {
                Text(
                    item.medications.joinToString("، ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
