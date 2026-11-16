package com.zenvairo.sportssuitability.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zenvairo.sportssuitability.data.local.Spot
import com.zenvairo.sportssuitability.data.local.SportType

/** Compass points offered for the coast orientation, with their bearing in degrees. */
private val COMPASS_CHOICES = listOf(
    "N" to 0.0, "NE" to 45.0, "E" to 90.0, "SE" to 135.0,
    "S" to 180.0, "SO" to 225.0, "O" to 270.0, "NO" to 315.0
)

/**
 * Per-spot tuning: which way the coast looks (so offshore wind can be detected) and, for ski
 * resorts, the altitude the forecast should be taken at.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpotSettingsDialog(
    spot: Spot,
    onSetCoastFacing: (Double?) -> Unit,
    onSetAltitude: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var altitudeText by remember(spot.id) {
        mutableStateOf(spot.altitudeM?.let { it.toInt().toString() } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spot.name) },
        text = {
            Column {
                if (spot.sportType != SportType.SKI) {
                    Text("Orientación de la costa", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Hacia dónde miras cuando tienes el mar delante. Con esto la app " +
                            "distingue viento de tierra (offshore) de viento de mar (onshore).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        COMPASS_CHOICES.forEach { (label, degrees) ->
                            FilterChip(
                                selected = spot.coastFacingDeg == degrees,
                                onClick = {
                                    onSetCoastFacing(if (spot.coastFacingDeg == degrees) null else degrees)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (spot.coastFacingDeg == null) {
                        Text(
                            "Sin definir: la dirección del viento no puntúa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text("Altitud de la estación", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "La previsión se pide a esta altura. Si dejas la del pueblo, verás " +
                            "lluvia donde en pistas está nevando.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = altitudeText,
                        onValueChange = { text ->
                            altitudeText = text.filter { it.isDigit() }
                            onSetAltitude(altitudeText.toDoubleOrNull())
                        },
                        label = { Text("Altitud") },
                        suffix = { Text("m") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
    )
}
