package com.zenvairo.sportssuitability.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.vector.ImageVector
import com.zenvairo.sportssuitability.data.local.SportType

data class SportUiInfo(val label: String, val icon: ImageVector)

fun SportType.uiInfo(): SportUiInfo = when (this) {
    SportType.SURF -> SportUiInfo("Surf", Icons.Filled.Waves)
    SportType.WINDSURF -> SportUiInfo("Windsurf", Icons.Filled.Air)
    SportType.SKI -> SportUiInfo("Esquí", Icons.Filled.AcUnit)
}
