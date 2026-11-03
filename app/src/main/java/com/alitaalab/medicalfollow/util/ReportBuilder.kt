package com.alitaalab.medicalfollow.util

import android.content.Context
import android.content.Intent
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.data.Child
import com.alitaalab.medicalfollow.data.Vaccine
import com.alitaalab.medicalfollow.data.VisitWithMeds

/** Builds a plain-text medical summary that can be shared with a doctor (WhatsApp, e-mail…). */
object ReportBuilder {

    fun ageText(context: Context, age: Age): String = when {
        age.years > 0 -> context.getString(R.string.age_years_months, age.years, age.months)
        age.months > 0 -> context.getString(R.string.age_months_days, age.months, age.days)
        else -> context.getString(R.string.age_days, age.days)
    }

    fun visitText(context: Context, item: VisitWithMeds): String = buildString {
        val v = item.visit
        append("📅 ").append(v.date.formatted())
        if (v.time.isNotBlank()) append(" ").append(v.time)
        appendLine()
        val doctor = listOf(v.doctorName, v.specialty, v.clinic).filter { it.isNotBlank() }.joinToString(" - ")
        if (doctor.isNotBlank()) appendLine("${context.getString(R.string.doctor)}: $doctor")
        if (v.reason.isNotBlank()) appendLine("${context.getString(R.string.reason)}: ${v.reason}")
        if (v.diagnosis.isNotBlank()) appendLine("${context.getString(R.string.diagnosis)}: ${v.diagnosis}")
        v.weightKg?.let { appendLine("${context.getString(R.string.weight_kg)}: ${it.pretty()}") }
        v.heightCm?.let { appendLine("${context.getString(R.string.height_cm)}: ${it.pretty()}") }
        v.temperatureC?.let { appendLine("${context.getString(R.string.temperature_c)}: ${it.pretty()}") }
        if (item.medications.isNotEmpty()) {
            appendLine("${context.getString(R.string.prescription)}:")
            item.medications.forEach { m ->
                val details = listOf(m.dose, m.frequency, m.duration).filter { it.isNotBlank() }.joinToString(" - ")
                appendLine("  💊 ${m.name}" + if (details.isNotBlank()) " ($details)" else "")
            }
        }
        if (v.notes.isNotBlank()) appendLine("${context.getString(R.string.notes)}: ${v.notes}")
    }.trimEnd()

    fun fullReport(
        context: Context,
        child: Child,
        vaccines: List<Vaccine>,
        visits: List<VisitWithMeds>,
    ): String = buildString {
        appendLine(context.getString(R.string.report_title, child.name))
        appendLine("${context.getString(R.string.birth_date)}: ${child.birthDate.formatted()}")
        appendLine("${context.getString(R.string.age)}: ${ageText(context, ageOn(child.birthDate))}")
        if (child.bloodType.isNotBlank()) appendLine("${context.getString(R.string.blood_type)}: ${child.bloodType}")
        if (child.allergies.isNotBlank()) appendLine("${context.getString(R.string.allergies)}: ${child.allergies}")
        appendLine()

        val taken = vaccines.filter { it.isTaken }.sortedBy { it.takenDate }
        val remaining = vaccines.filter { !it.isTaken }.sortedBy { it.dueDate }
        appendLine("✅ ${context.getString(R.string.vaccines_taken)} (${taken.size})")
        taken.forEach { appendLine("  • ${it.name} — ${it.takenDate!!.formatted()}") }
        appendLine()
        appendLine("⏳ ${context.getString(R.string.vaccines_remaining)} (${remaining.size})")
        remaining.forEach { appendLine("  • ${it.name} — ${it.dueDate.formatted()}") }
        appendLine()

        appendLine("🩺 ${context.getString(R.string.tab_visits)} (${visits.size})")
        visits.sortedByDescending { it.visit.date }.forEach {
            appendLine("—————")
            appendLine(visitText(context, it))
        }
    }.trimEnd()

    fun share(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share)))
    }
}
