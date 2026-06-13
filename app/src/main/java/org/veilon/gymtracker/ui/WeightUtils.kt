package org.veilon.gymtracker.ui

import kotlin.math.roundToInt

private const val LBS_PER_KG = 2.20462

// Full display with unit, e.g. "102 kg" or "225 lbs" — for read-only views
fun formatWeight(kg: Double, useLbs: Boolean): String {
    return if (useLbs) "${(kg * LBS_PER_KG).roundToInt()} lbs"
    else "${(kg * 10).roundToInt() / 10.0} kg"
}

// Just the rounded number (no unit) — for prefilling editable text fields
fun displayWeight(kg: Double, useLbs: Boolean): String {
    return if (useLbs) (kg * LBS_PER_KG).roundToInt().toString()
    else ((kg * 10).roundToInt() / 10.0).toString()
}

// Convert a user-entered value (in their chosen unit) back to kg for storage
fun toKg(value: Double, useLbs: Boolean): Double {
    return if (useLbs) value / LBS_PER_KG else value
}