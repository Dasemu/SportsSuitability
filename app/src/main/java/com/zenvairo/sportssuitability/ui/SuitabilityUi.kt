package com.zenvairo.sportssuitability.ui

import androidx.compose.ui.graphics.Color
import com.zenvairo.sportssuitability.data.repository.Suitability

/** Traffic-light colours for each verdict, kept out of the rules engine. */
val Suitability.color: Color
    get() = when (this) {
        Suitability.EXCELLENT -> Color(0xFF1B7F3B)
        Suitability.GOOD -> Color(0xFF5FA531)
        Suitability.FAIR -> Color(0xFFE0A100)
        Suitability.POOR -> Color(0xFFD2622B)
        Suitability.NO_GO -> Color(0xFFB3261E)
        Suitability.UNKNOWN -> Color(0xFF8A8A8A)
    }
