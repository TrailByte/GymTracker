package org.veilon.gymtracker.ui

fun formatWeight(kg: Double, useLbs: Boolean): String {
    return if (useLbs) {
        val lbs = kg * 2.20462
        "${String.format("%.1f", lbs)} lbs"
    } else {
        "${String.format("%.1f", kg)} kg"
    }
}

fun toKg(value: Double, useLbs: Boolean): Double {
    return if (useLbs) value / 2.20462 else value
}