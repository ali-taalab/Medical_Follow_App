package com.alitaalab.medicalfollow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.data.Vaccine
import com.alitaalab.medicalfollow.ui.AppViewModel
import com.alitaalab.medicalfollow.ui.VaccineStatus
import com.alitaalab.medicalfollow.ui.components.DateField
import com.alitaalab.medicalfollow.ui.components.EmptyState
import com.alitaalab.medicalfollow.ui.components.FormField
import com.alitaalab.medicalfollow.ui.components.SectionTitle
import com.alitaalab.medicalfollow.ui.components.StatusChip
import com.alitaalab.medicalfollow.ui.components.statusColor
import com.alitaalab.medicalfollow.ui.status
import com.alitaalab.medicalfollow.util.formatted
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class VaccineFilter { ALL, REMAINING, TAKEN }

/** Group key: scheduled doses are grouped by age; custom ones go into a final group. */
private fun groupKey(v: Vaccine): Int =
    if (v.offsetMonths == null) Int.MAX_VALUE else v.offsetMonths * 100 + if (v.offsetDays > 0) 1 else 0

@Composable
private fun groupLabel(key: Int): String {
    if (key == Int.MAX_VALUE) return stringResource(R.string.group_custom)
    val months = key / 100
    val days = key % 100
    return when {
        months == 0 && days == 0 -> stringResource(R.string.group_birth)
        months == 0 -> stringResource(R.string.group_first_month)
        else -> stringResource(R.string.group_months, months)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinesScreen(vm: AppViewModel) {
    val vaccines by vm.vaccines.collectAsStateWithLifecycle()
    val childState by vm.childState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(VaccineFilter.ALL) }
    var editing by remember { mutableStateOf<Vaccine?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val today = LocalDate.now()

    val filtered = when (filter) {
        VaccineFilter.ALL -> vaccines
        VaccineFilter.REMAINING -> vaccines.filter { !it.isTaken }
        VaccineFilter.TAKEN -> vaccines.filter { it.isTaken }
    }
    val groups = filtered.groupBy(::groupKey).toSortedMap()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_vaccines)) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_optional_vaccines)) },
                            onClick = {
                                menuOpen = false
                                vm.addOptionalVaccines { count ->
                                    scope.launch {
                                        snackbar.showSnackbar(context.getString(R.string.optional_added, count))
                                    }
                                }
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = Vaccine(id = 0, name = "", dueDate = today, mandatory = false)
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_vaccine))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter == VaccineFilter.ALL,
                    onClick = { filter = VaccineFilter.ALL },
                    label = { Text(stringResource(R.string.filter_all, vaccines.size)) },
                )
                FilterChip(
                    selected = filter == VaccineFilter.REMAINING,
                    onClick = { filter = VaccineFilter.REMAINING },
                    label = { Text(stringResource(R.string.filter_remaining, vaccines.count { !it.isTaken })) },
                )
                FilterChip(
                    selected = filter == VaccineFilter.TAKEN,
                    onClick = { filter = VaccineFilter.TAKEN },
                    label = { Text(stringResource(R.string.filter_taken, vaccines.count { it.isTaken })) },
                )
            }
            if (filtered.isEmpty()) {
                EmptyState(Icons.Default.Vaccines, stringResource(R.string.no_vaccines))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groups.forEach { (key, list) ->
                        item(key = "header_$key") { SectionTitle(groupLabel(key)) }
                        items(list, key = { it.id }) { v ->
                            VaccineCard(
                                v = v,
                                today = today,
                                onToggle = {
                                    vm.saveVaccine(v.copy(takenDate = if (v.isTaken) null else today))
                                },
                                onClick = { editing = v },
                            )
                        }
                    }
                    item(key = "disclaimer") {
                        Text(
                            stringResource(R.string.schedule_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
        }
    }

    editing?.let { v ->
        VaccineDialog(
            initial = v,
            birthDate = childState.child?.birthDate,
            onDismiss = { editing = null },
            onSave = { vm.saveVaccine(it); editing = null },
            onDelete = { vm.deleteVaccine(it); editing = null },
        )
    }
}

@Composable
private fun VaccineCard(v: Vaccine, today: LocalDate, onToggle: () -> Unit, onClick: () -> Unit) {
    val status = v.status(today)
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (v.isTaken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = stringResource(R.string.mark_taken),
                    tint = statusColor(status),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(v.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (v.description.isNotBlank()) {
                    Text(v.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (v.isTaken) stringResource(R.string.taken_on, v.takenDate!!.formatted())
                    else stringResource(R.string.due_on, v.dueDate.formatted()),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor(status),
                )
                if (v.notes.isNotBlank()) {
                    Text("📝 ${v.notes}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(status)
        }
    }
}

@Composable
private fun VaccineDialog(
    initial: Vaccine,
    birthDate: LocalDate?,
    onDismiss: () -> Unit,
    onSave: (Vaccine) -> Unit,
    onDelete: (Vaccine) -> Unit,
) {
    val isNew = initial.id == 0L
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var description by rememberSaveable { mutableStateOf(initial.description) }
    var dueEpoch by rememberSaveable { mutableStateOf(initial.dueDate.toEpochDay()) }
    var taken by rememberSaveable { mutableStateOf(initial.isTaken) }
    var takenEpoch by rememberSaveable { mutableStateOf((initial.takenDate ?: LocalDate.now()).toEpochDay()) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.add_vaccine else R.string.edit_vaccine)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormField(name, { name = it }, stringResource(R.string.vaccine_name), singleLine = false)
                FormField(description, { description = it }, stringResource(R.string.description), singleLine = false)
                DateField(
                    label = stringResource(R.string.due_date),
                    date = LocalDate.ofEpochDay(dueEpoch),
                    onDateChange = { it?.let { d -> dueEpoch = d.toEpochDay() } },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.mark_taken), modifier = Modifier.weight(1f))
                    Switch(checked = taken, onCheckedChange = { taken = it })
                }
                if (taken) {
                    DateField(
                        label = stringResource(R.string.taken_date),
                        date = LocalDate.ofEpochDay(takenEpoch),
                        onDateChange = { it?.let { d -> takenEpoch = d.toEpochDay() } },
                    )
                }
                FormField(notes, { notes = it }, stringResource(R.string.vaccine_notes_hint), singleLine = false)
                if (!isNew) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val due = LocalDate.ofEpochDay(dueEpoch)
                    // If the user moves the due date of a scheduled dose, treat it as custom
                    // so it is not overwritten when the birth date changes.
                    val keepSchedule = initial.offsetMonths != null && due == initial.dueDate
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            description = description.trim(),
                            dueDate = due,
                            offsetMonths = if (keepSchedule) initial.offsetMonths else null,
                            takenDate = if (taken) LocalDate.ofEpochDay(takenEpoch) else null,
                            notes = notes.trim(),
                        )
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.confirm_delete_vaccine, initial.name)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(initial) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
