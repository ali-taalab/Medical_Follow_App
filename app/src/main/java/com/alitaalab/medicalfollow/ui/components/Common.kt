package com.alitaalab.medicalfollow.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.ui.VaccineStatus
import com.alitaalab.medicalfollow.ui.theme.StatusColors
import com.alitaalab.medicalfollow.util.Age
import com.alitaalab.medicalfollow.util.formatted
import com.alitaalab.medicalfollow.util.toUtcMillis
import com.alitaalab.medicalfollow.util.utcMillisToLocalDate
import java.time.LocalDate

@Composable
fun ageText(age: Age): String = when {
    age.years > 0 -> stringResource(R.string.age_years_months, age.years, age.months)
    age.months > 0 -> stringResource(R.string.age_months_days, age.months, age.days)
    else -> stringResource(R.string.age_days, age.days)
}

/** Read-only text field that opens a Material date picker when tapped. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
) {
    var show by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        interaction.interactions.collect { if (it is PressInteraction.Release) show = true }
    }
    OutlinedTextField(
        value = date?.formatted() ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            if (allowClear && date != null) {
                IconButton(onClick = { onDateChange(null) }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                }
            } else {
                IconButton(onClick = { show = true }) {
                    Icon(Icons.Default.Event, contentDescription = label)
                }
            }
        },
        interactionSource = interaction,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (date ?: LocalDate.now()).toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(it.utcMillisToLocalDate()) }
                    show = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

fun statusColor(status: VaccineStatus): Color = when (status) {
    VaccineStatus.TAKEN -> StatusColors.taken
    VaccineStatus.OVERDUE -> StatusColors.overdue
    VaccineStatus.DUE_SOON -> StatusColors.dueSoon
    VaccineStatus.UPCOMING -> StatusColors.upcoming
}

@Composable
fun statusLabel(status: VaccineStatus): String = when (status) {
    VaccineStatus.TAKEN -> stringResource(R.string.status_taken)
    VaccineStatus.OVERDUE -> stringResource(R.string.status_overdue)
    VaccineStatus.DUE_SOON -> stringResource(R.string.status_due_soon)
    VaccineStatus.UPCOMING -> stringResource(R.string.status_upcoming)
}

@Composable
fun StatusChip(status: VaccineStatus) {
    val color = statusColor(status)
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = statusLabel(status),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
fun LabeledValue(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyState(icon: ImageVector, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(8.dp).padding(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
