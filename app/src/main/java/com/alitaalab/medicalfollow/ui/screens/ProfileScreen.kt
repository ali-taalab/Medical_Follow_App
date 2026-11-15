package com.alitaalab.medicalfollow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.data.Child
import com.alitaalab.medicalfollow.ui.AppViewModel
import com.alitaalab.medicalfollow.ui.components.DateField
import com.alitaalab.medicalfollow.ui.components.FormField
import com.alitaalab.medicalfollow.ui.components.ageText
import com.alitaalab.medicalfollow.util.ageOn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Used both for the first-run setup and for editing the profile later. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: AppViewModel, child: Child?, isSetup: Boolean) {
    var name by rememberSaveable(child) { mutableStateOf(child?.name ?: "") }
    var birthEpoch by rememberSaveable(child) { mutableStateOf(child?.birthDate?.toEpochDay()) }
    var gender by rememberSaveable(child) { mutableStateOf(child?.gender ?: Child.GENDER_MALE) }
    var bloodType by rememberSaveable(child) { mutableStateOf(child?.bloodType ?: "") }
    var allergies by rememberSaveable(child) { mutableStateOf(child?.allergies ?: "") }
    var notes by rememberSaveable(child) { mutableStateOf(child?.notes ?: "") }
    var showErrors by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.saved)
    val birth = birthEpoch?.let { LocalDate.ofEpochDay(it) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(if (isSetup) R.string.setup_title else R.string.tab_profile)) })
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = if (isSetup) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isSetup) {
                Text(stringResource(R.string.setup_intro), style = MaterialTheme.typography.bodyLarge)
            }
            FormField(name, { name = it }, stringResource(R.string.child_name))
            if (showErrors && name.isBlank()) {
                Text(stringResource(R.string.error_name), color = MaterialTheme.colorScheme.error)
            }
            DateField(
                label = stringResource(R.string.birth_date),
                date = birth,
                onDateChange = { birthEpoch = it?.toEpochDay() },
            )
            if (showErrors && birth == null) {
                Text(stringResource(R.string.error_birth), color = MaterialTheme.colorScheme.error)
            }
            if (birth != null) {
                Text(
                    "${stringResource(R.string.age)}: ${ageText(ageOn(birth))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = gender == Child.GENDER_MALE,
                    onClick = { gender = Child.GENDER_MALE },
                    label = { Text(stringResource(R.string.male)) },
                )
                FilterChip(
                    selected = gender == Child.GENDER_FEMALE,
                    onClick = { gender = Child.GENDER_FEMALE },
                    label = { Text(stringResource(R.string.female)) },
                )
            }
            FormField(bloodType, { bloodType = it }, stringResource(R.string.blood_type))
            FormField(allergies, { allergies = it }, stringResource(R.string.allergies), singleLine = false)
            FormField(notes, { notes = it }, stringResource(R.string.notes), singleLine = false, minLines = 3)

            if (!isSetup && child != null && birth != null && birth != child.birthDate) {
                Text(
                    stringResource(R.string.birth_change_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    if (name.isBlank() || birth == null) {
                        showErrors = true
                        return@Button
                    }
                    vm.saveChild(
                        Child(
                            name = name.trim(),
                            birthDate = birth,
                            gender = gender,
                            bloodType = bloodType.trim(),
                            allergies = allergies.trim(),
                            notes = notes.trim(),
                        )
                    )
                    if (!isSetup) scope.launch { snackbar.showSnackbar(savedMsg) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (isSetup) R.string.start else R.string.save))
            }
        }
    }
}
