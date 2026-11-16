package com.zenvairo.sportssuitability.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zenvairo.sportssuitability.alerts.dayOfWeekLabel
import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.prefs.AppSettings
import com.zenvairo.sportssuitability.data.prefs.Limit
import com.zenvairo.sportssuitability.data.prefs.LimitTunable
import com.zenvairo.sportssuitability.data.prefs.SnowUnit
import com.zenvairo.sportssuitability.data.prefs.TempUnit
import com.zenvairo.sportssuitability.data.prefs.Tunable
import com.zenvairo.sportssuitability.data.prefs.TunableGroup
import com.zenvairo.sportssuitability.data.prefs.ValueTunable
import com.zenvairo.sportssuitability.data.prefs.WaveUnit
import com.zenvairo.sportssuitability.data.prefs.WindUnit
import com.zenvairo.sportssuitability.data.prefs.fromDisplay
import com.zenvairo.sportssuitability.data.prefs.suffix
import com.zenvairo.sportssuitability.data.prefs.toDisplay
import com.zenvairo.sportssuitability.data.prefs.tunableGroups
import com.zenvairo.sportssuitability.ui.uiInfo
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onResetSport: (SportType) -> Unit,
    onTestDailyAlert: () -> Unit,
    onTestWeeklyAlert: () -> Unit
) {
    // Bumped whenever a change should force every text field to re-read from settings
    // (unit switches, "restore defaults"), without fighting the user mid-keystroke.
    var resetToken by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection("Unidades") {
                LabelledUnitRow("Viento", WindUnit.values().toList(), settings.windUnit, { it.label }) { unit ->
                    resetToken++
                    onUpdate { it.copy(windUnit = unit) }
                }
                LabelledUnitRow("Olas", WaveUnit.values().toList(), settings.waveUnit, { it.label }) { unit ->
                    resetToken++
                    onUpdate { it.copy(waveUnit = unit) }
                }
                LabelledUnitRow("Temperatura", TempUnit.values().toList(), settings.tempUnit, { it.label }) { unit ->
                    resetToken++
                    onUpdate { it.copy(tempUnit = unit) }
                }
                LabelledUnitRow("Nieve", SnowUnit.values().toList(), settings.snowUnit, { it.label }) { unit ->
                    resetToken++
                    onUpdate { it.copy(snowUnit = unit) }
                }
            }
        }

        item {
            SettingsSection(
                title = "Exigencia",
                description = "Puntuación mínima (0–100) para que la app diga que sí puedes ir. " +
                    "Súbela si sólo quieres saber de los días realmente buenos."
            ) {
                Text(
                    "Mínimo para ir: ${settings.minScoreToGo}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = settings.minScoreToGo.toFloat(),
                    onValueChange = { value ->
                        onUpdate { it.copy(minScoreToGo = value.roundToInt()) }
                    },
                    valueRange = 0f..100f,
                    steps = 19
                )
            }
        }

        item { AlertsSection(settings, onUpdate, onTestDailyAlert, onTestWeeklyAlert) }

        SportType.values().forEach { sport ->
            item {
                SportRulesHeader(sport) {
                    resetToken++
                    onResetSport(sport)
                }
            }
            item { SportPreferences(sport, settings, onUpdate) }
            tunableGroups(sport).forEach { group ->
                item {
                    TunableGroupCard(
                        group = group,
                        settings = settings,
                        resetToken = resetToken,
                        onUpdate = onUpdate
                    )
                }
            }
        }

        item {
            Text(
                "Los umbrales se guardan siempre en unidades métricas, así que cambiar de " +
                    "unidad no altera ninguna regla.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Alerts
// ---------------------------------------------------------------------------

@Composable
private fun AlertsSection(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onTestDailyAlert: () -> Unit,
    onTestWeeklyAlert: () -> Unit
) {
    val alerts = settings.alerts
    SettingsSection("Avisos") {
        SwitchRow(
            title = "Aviso del día",
            subtitle = "Cada mañana te digo si hoy puedes ir a los sitios que hayas marcado.",
            checked = alerts.dailyEnabled
        ) { enabled -> onUpdate { it.copy(alerts = it.alerts.copy(dailyEnabled = enabled)) } }

        if (alerts.dailyEnabled) {
            TimeRow(
                hour = alerts.dailyHour,
                minute = alerts.dailyMinute,
                onHourChange = { h -> onUpdate { it.copy(alerts = it.alerts.copy(dailyHour = h)) } },
                onMinuteChange = { m -> onUpdate { it.copy(alerts = it.alerts.copy(dailyMinute = m)) } }
            )
            SwitchRow(
                title = "Sólo si puedo ir",
                subtitle = "Desactivado, también te aviso cuando no merece la pena, con el motivo.",
                checked = alerts.dailyOnlyWhenGood
            ) { only -> onUpdate { it.copy(alerts = it.alerts.copy(dailyOnlyWhenGood = only)) } }
        }

        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        SwitchRow(
            title = "Previsión semanal",
            subtitle = "Un resumen de la semana que viene con los mejores días de cada sitio.",
            checked = alerts.weeklyEnabled
        ) { enabled -> onUpdate { it.copy(alerts = it.alerts.copy(weeklyEnabled = enabled)) } }

        if (alerts.weeklyEnabled) {
            Text("Día", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            WeekdayPicker(alerts.weeklyDayOfWeek) { day ->
                onUpdate { it.copy(alerts = it.alerts.copy(weeklyDayOfWeek = day)) }
            }
            TimeRow(
                hour = alerts.weeklyHour,
                minute = alerts.weeklyMinute,
                onHourChange = { h -> onUpdate { it.copy(alerts = it.alerts.copy(weeklyHour = h)) } },
                onMinuteChange = { m -> onUpdate { it.copy(alerts = it.alerts.copy(weeklyMinute = m)) } }
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onTestDailyAlert, modifier = Modifier.weight(1f)) {
                Text("Probar diario")
            }
            OutlinedButton(onClick = onTestWeeklyAlert, modifier = Modifier.weight(1f)) {
                Text("Probar semanal")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayPicker(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (1..7).forEach { day ->
            FilterChip(
                selected = selected == day,
                onClick = { onSelect(day) },
                label = { Text(dayOfWeekLabel(day).take(3)) }
            )
        }
    }
}

@Composable
private fun TimeRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IntField("Hora", hour, 0, 23, Modifier.weight(1f), onHourChange)
        IntField("Minuto", minute, 0, 59, Modifier.weight(1f), onMinuteChange)
    }
}

@Composable
private fun IntField(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(2)
            text = digits
            digits.toIntOrNull()?.takeIf { it in min..max }?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------
// Per-sport rules
// ---------------------------------------------------------------------------

@Composable
private fun SportRulesHeader(sport: SportType, onReset: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Reglas de ${sport.uiInfo().label}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedButton(onClick = onReset) { Text("Restaurar") }
    }
}

@Composable
private fun SportPreferences(
    sport: SportType,
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit
) {
    when (sport) {
        SportType.SURF -> SettingsSection("Dirección del viento") {
            SwitchRow(
                title = "Premiar viento offshore",
                subtitle = "Necesita que hayas fijado la orientación de la costa en cada sitio.",
                checked = settings.surf.preferOffshore
            ) { value -> onUpdate { it.copy(surf = it.surf.copy(preferOffshore = value)) } }
        }
        SportType.WINDSURF -> SettingsSection("Dirección del viento") {
            SwitchRow(
                title = "Premiar viento cruzado",
                subtitle = "Penaliza el offshore puro, que te empuja mar adentro.",
                checked = settings.windsurf.preferSideShore
            ) { value -> onUpdate { it.copy(windsurf = it.windsurf.copy(preferSideShore = value)) } }
        }
        SportType.SKI -> Unit
    }
}

@Composable
private fun TunableGroupCard(
    group: TunableGroup,
    settings: AppSettings,
    resetToken: Int,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit
) {
    SettingsSection(group.title, group.description) {
        group.items.forEach { tunable ->
            TunableRow(tunable, settings, resetToken, onUpdate)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun TunableRow(
    tunable: Tunable,
    settings: AppSettings,
    resetToken: Int,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit
) {
    val suffix = tunable.kind.suffix(settings)

    when (tunable) {
        is ValueTunable -> {
            DoubleField(
                fieldKey = "${tunable.id}-$resetToken-$suffix",
                label = tunable.label,
                help = tunable.help,
                displayValue = tunable.kind.toDisplay(tunable.get(settings), settings),
                suffix = suffix,
                enabled = true,
                onChange = { typed ->
                    onUpdate { current ->
                        tunable.set(current, tunable.kind.fromDisplay(typed, current))
                    }
                }
            )
        }

        is LimitTunable -> {
            val limit = tunable.get(settings)
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tunable.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (limit.enabled) "Regla activa: impide ir" else "Regla desactivada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = limit.enabled,
                        onCheckedChange = { enabled ->
                            onUpdate { current ->
                                tunable.set(current, tunable.get(current).copy(enabled = enabled))
                            }
                        }
                    )
                }
                tunable.help?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DoubleField(
                    fieldKey = "${tunable.id}-$resetToken-$suffix",
                    label = "Valor",
                    help = null,
                    displayValue = tunable.kind.toDisplay(limit.value, settings),
                    suffix = suffix,
                    enabled = limit.enabled,
                    onChange = { typed ->
                        onUpdate { current ->
                            tunable.set(
                                current,
                                Limit(
                                    enabled = tunable.get(current).enabled,
                                    value = tunable.kind.fromDisplay(typed, current)
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * A decimal input that keeps its own text while being edited. [fieldKey] carries the reset
 * token and the unit suffix, so the field re-reads from settings when either changes but
 * never while the user is halfway through typing "1.".
 */
@Composable
private fun DoubleField(
    fieldKey: String,
    label: String,
    help: String?,
    displayValue: Double,
    suffix: String,
    enabled: Boolean,
    onChange: (Double) -> Unit
) {
    var text by remember(fieldKey) { mutableStateOf(formatValue(displayValue)) }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                input.replace(',', '.').toDoubleOrNull()?.let(onChange)
            },
            label = { Text(label) },
            suffix = { Text(suffix) },
            enabled = enabled,
            singleLine = true,
            isError = text.replace(',', '.').toDoubleOrNull() == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        help?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Small building blocks
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSection(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> LabelledUnitRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(optionLabel(option)) }
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatValue(v: Double): String {
    val rounded = "%.1f".format(v)
    return if (rounded.endsWith(".0")) rounded.dropLast(2) else rounded
}
