package com.example.todolist.ui.theme

import androidx.compose.ui.graphics.Color

val Indigo900 = Color(0xFF1A237E)
val Indigo700 = Color(0xFF303F9F)
val Indigo500 = Color(0xFF3F51B5)
val Indigo200 = Color(0xFF9FA8DA)
val Indigo100 = Color(0xFFC5CAE9)

val Blue800 = Color(0xFF1565C0)
val Blue600 = Color(0xFF1E88E5)
val Blue200 = Color(0xFF90CAF9)

val SurfaceLight = Color(0xFFF8F9FF)
val SurfaceDark = Color(0xFF121318)
val OnSurfaceDark = Color(0xFFE2E2EC)

// Стандартная схема
val PriorityLow = Color(0xFF4CAF50)
val PriorityMedium = Color(0xFFFF9800)
val PriorityHigh = Color(0xFFF44336)

// Пастельная схема
val PasteLow = Color(0xFF81C784)
val PasteMedium = Color(0xFFFFCC80)
val PasteHigh = Color(0xFFEF9A9A)

// Монохромная схема
val MonoLow = Color(0xFF9E9E9E)
val MonoMedium = Color(0xFF616161)
val MonoHigh = Color(0xFF212121)

data class PriorityColors(val low: Color, val medium: Color, val high: Color) {
    companion object {
        val Standard = PriorityColors(PriorityLow, PriorityMedium, PriorityHigh)
        val Pastel = PriorityColors(PasteLow, PasteMedium, PasteHigh)
        val Monochrome = PriorityColors(MonoLow, MonoMedium, MonoHigh)

        fun fromScheme(index: Int) = when (index) {
            1 -> Pastel
            2 -> Monochrome
            else -> Standard
        }
    }
}
