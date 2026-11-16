package com.zenvairo.sportssuitability.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenvairo.sportssuitability.data.prefs.formatAltitude
import com.zenvairo.sportssuitability.data.remote.GeocodingResult
import com.zenvairo.sportssuitability.ui.SearchUiState

@Composable
fun AddSpotDialog(
    sportLabel: String,
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (GeocodingResult) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir sitio de $sportLabel") },
        text = {
            Column {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChanged,
                    label = { Text("Nombre del lugar") },
                    supportingText = { Text("Escribe y espera; la búsqueda es automática") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (state.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(state.results, key = { it.id }) { result ->
                            ListItem(
                                headlineContent = { Text(result.name) },
                                supportingContent = {
                                    val details = listOfNotNull(
                                        result.regionLabel,
                                        result.elevation?.formatAltitude()
                                    ).joinToString(" · ")
                                    if (details.isNotBlank()) Text(details)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(result) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSearch) { Text("Buscar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
