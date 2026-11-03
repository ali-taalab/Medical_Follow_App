package com.alitaalab.medicalfollow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.data.Vaccine
import com.alitaalab.medicalfollow.data.VisitWithMeds
import com.alitaalab.medicalfollow.ui.AppViewModel
import com.alitaalab.medicalfollow.ui.VaccineStatus
import com.alitaalab.medicalfollow.ui.components.SectionTitle
import com.alitaalab.medicalfollow.ui.components.StatusChip
import com.alitaalab.medicalfollow.ui.components.ageText
import com.alitaalab.medicalfollow.ui.status
import com.alitaalab.medicalfollow.ui.theme.StatusColors
import com.alitaalab.medicalfollow.util.ReportBuilder
import com.alitaalab.medicalfollow.util.ageOn
import com.alitaalab.medicalfollow.util.formatted
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenVaccines: () -> Unit,
    onOpenVisit: (Long) -> Unit,
    onAddVisit: () -> Unit,
) {
    val childState by vm.childState.collectAsStateWithLifecycle()
    val vaccines by vm.vaccines.collectAsStateWithLifecycle()
    val visits by vm.visits.collectAsStateWithLifecycle()
    val child = childState.child ?: return
    val context = LocalContext.current
    val today = LocalDate.now()

    val taken = vaccines.count { it.isTaken }
    val overdue = vaccines.filter { it.status(today) == VaccineStatus.OVERDUE }
    val next = vaccines.filter { !it.isTaken && !it.dueDate.isBefore(today) }.take(4)
    val upcomingVisits = visits.filter { !it.visit.date.isBefore(today) }.sortedBy { it.visit.date }.take(3)
    val lastVisit = visits.firstOrNull { it.visit.date.isBefore(today) || it.visit.date == today }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = {
                        ReportBuilder.share(context, ReportBuilder.fullReport(context, child, vaccines, visits))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_report))
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Child header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ChildCare,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(child.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(ageText(ageOn(child.birthDate)), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${stringResource(R.string.birth_date)}: ${child.birthDate.formatted()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // Vaccine progress
            if (vaccines.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenVaccines)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.vaccine_progress, taken, vaccines.size),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { taken.toFloat() / vaccines.size },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Overdue warning
            if (overdue.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusColors.overdue.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenVaccines),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusColors.overdue)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.overdue_count, overdue.size),
                                fontWeight = FontWeight.Bold,
                                color = StatusColors.overdue,
                            )
                        }
                        overdue.take(5).forEach {
                            Text("• ${it.name} (${it.dueDate.formatted()})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.next_vaccines))
            if (next.isEmpty()) {
                Text(stringResource(R.string.no_upcoming_vaccines), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                next.forEach { VaccineMiniRow(it, today, onOpenVaccines) }
            }

            SectionTitle(stringResource(R.string.upcoming_visits))
            if (upcomingVisits.isEmpty()) {
                Text(stringResource(R.string.no_upcoming_visits), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                upcomingVisits.forEach { VisitMiniRow(it) { onOpenVisit(it.visit.id) } }
            }
            OutlinedButton(onClick = onAddVisit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_visit))
            }

            if (lastVisit != null) {
                SectionTitle(stringResource(R.string.last_visit))
                VisitMiniRow(lastVisit) { onOpenVisit(lastVisit.visit.id) }
            }
        }
    }
}

@Composable
private fun VaccineMiniRow(v: Vaccine, today: LocalDate, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(v.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(v.dueDate.formatted(), style = MaterialTheme.typography.bodySmall)
            }
            StatusChip(v.status(today))
        }
    }
}

@Composable
fun VisitMiniRow(item: VisitWithMeds, onClick: () -> Unit) {
    val v = item.visit
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val title = listOf(v.doctorName, v.specialty).filter { it.isNotBlank() }.joinToString(" - ")
                Text(
                    title.ifBlank { v.reason.ifBlank { stringResource(R.string.visit) } },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    v.date.formatted() + if (v.time.isNotBlank()) " • ${v.time}" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (v.diagnosis.isNotBlank()) {
                    Text(v.diagnosis, style = MaterialTheme.typography.bodySmall, color = Color.Unspecified)
                }
            }
        }
    }
}
