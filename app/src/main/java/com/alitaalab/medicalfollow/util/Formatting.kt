package com.alitaalab.medicalfollow.util

import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LocalDate.formatted(): String =
    format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

/** Material3 date pickers work with UTC-midnight milliseconds. */
fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.utcMillisToLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Converts Arabic-Indic / Persian digits and decimal separators so numbers parse correctly. */
fun normalizeDigits(input: String): String = buildString {
    input.forEach { c ->
        append(
            when (c) {
                in '٠'..'٩' -> '0' + (c - '٠')
                in '۰'..'۹' -> '0' + (c - '۰')
                '٫', ',' -> '.'
                else -> c
            }
        )
    }
}

fun parseDecimal(input: String): Double? = normalizeDigits(input).trim().toDoubleOrNull()

fun Double.pretty(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')

data class Age(val years: Int, val months: Int, val days: Int)

fun ageOn(birth: LocalDate, on: LocalDate = LocalDate.now()): Age {
    if (on.isBefore(birth)) return Age(0, 0, 0)
    val p = Period.between(birth, on)
    return Age(p.years, p.months, p.days)
}
