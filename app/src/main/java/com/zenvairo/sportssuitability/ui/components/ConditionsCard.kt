package com.zenvairo.sportssuitability.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenvairo.sportssuitability.alerts.shortDayLabel
import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.prefs.AppSettings
import com.zenvairo.sportssuitability.data.prefs.formatSnow
import com.zenvairo.sportssuitability.data.prefs.formatSnowDepth
import com.zenvairo.sportssuitability.data.prefs.formatTemp
import com.zenvairo.sportssuitability.data.prefs.formatWave
import com.zenvairo.sportssuitability.data.prefs.formatWind
import com.zenvairo.sportssuitability.data.repository.DailyConditions
import com.zenvairo.sportssuitability.data.repository.SpotConditions
import com.zenvairo.sportssuitability.data.repository.Suitability
import com.zenvairo.sportssuitability.data.repository.Verdict
import com.zenvairo.sportssuitability.data.repository.bestWindow
import com.zenvairo.sportssuitability.data.repository.dailyVerdict
import com.zenvairo.sportssuitability.data.repository.nowVerdict
import com.zenvairo.sportssuitability.data.repository.weatherCodeEmoji
import com.zenvairo.sportssuitability.data.repository.weatherCodeText
import com.zenvairo.sportssuitability.ui.MetricSection
import com.zenvairo.sportssuitability.ui.headlineMetrics
import com.zenvairo.sportssuitability.ui.nowSections
import com.zenvairo.sportssuitability.ui.color
import java.time.LocalDate

@Composable
fun ConditionsCard(
    conditions: SpotConditions,
    settings: AppSettings,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    onToggleAlerts: (Boolean) -> Unit
) {
    var expanded by remember(conditions.spot.id) { mutableStateOf(false) }
    var selectedDay by remember(conditions.spot.id) { mutableStateOf<DailyConditions?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            CardHeader(conditions, settings, onEdit, onRemove)

            if (conditions.error != null) {
                Text(
                    "Sin datos: ${conditions.error}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            val verdict = conditions.nowVerdict(settings)
            Spacer(Modifier.height(10.dp))
            VerdictBlock(conditions, verdict, settings)

            conditions.partialWarning?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            val headline = conditions.headlineMetrics(settings)
            if (headline.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                MetricChips(headline)
            }

            val sections = conditions.nowSections(settings)
            if (sections.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ExpandToggle(
                    expanded = expanded,
                    collapsedLabel = "Ver todos los datos",
                    expandedLabel = "Ocultar detalle",
                    onToggle = { expanded = !expanded }
                )
                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        SectionList(sections)
                    }
                }
            }

            if (conditions.days.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text("Próximos días", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Toca un día para ver todos sus datos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                WeekStrip(conditions, settings) { selectedDay = it }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Avisarme por la mañana", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Te diré si hoy puedes ir a este sitio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = conditions.spot.alertsEnabled, onCheckedChange = onToggleAlerts)
            }
        }
    }

    selectedDay?.let { day ->
        DayDetailDialog(
            conditions = conditions,
            day = day,
            settings = settings,
            onDismiss = { selectedDay = null }
        )
    }
}

@Composable
private fun CardHeader(
    conditions: SpotConditions,
    settings: AppSettings,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                conditions.spot.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            conditions.spot.region?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            conditions.now?.let { now ->
                val sky = weatherCodeText(now.weatherCode)
                if (sky != null) {
                    Text(
                        "${weatherCodeEmoji(now.weatherCode, now.isDay ?: true)} $sky",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (conditions.error == null) {
            val verdict = conditions.nowVerdict(settings)
            SuitabilityBadge(verdict.suitability, verdict.score)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Tune, contentDescription = "Ajustes del sitio")
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar sitio")
        }
    }
}

/** The go / no-go answer, its score bar and the reasons behind it. */
@Composable
private fun VerdictBlock(conditions: SpotConditions, verdict: Verdict, settings: AppSettings) {
    val answer = when {
        verdict.suitability == Suitability.UNKNOWN -> "Sin datos suficientes"
        verdict.canGo -> "Hoy sí puedes ir"
        verdict.blockers.isNotEmpty() -> "Hoy no: hay algo que lo impide"
        else -> "Hoy mejor no: no llega al mínimo"
    }
    Text(answer, style = MaterialTheme.typography.titleSmall, color = verdict.suitability.color)

    verdict.score?.let {
        Spacer(Modifier.height(6.dp))
        ScoreBar(it, verdict.suitability.color)
    }

    conditions.today?.let { today ->
        conditions.bestWindow(today.date, settings)?.let { (from, to) ->
            Text(
                "Mejor franja de hoy: $from – $to",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }

    ReasonList("Por qué no", verdict.blockers, Suitability.NO_GO.color)
    ReasonList("A favor", verdict.highlights, Suitability.GOOD.color)
    ReasonList("Ojo", verdict.warnings, Suitability.FAIR.color)
}

@Composable
private fun ExpandToggle(
    expanded: Boolean,
    collapsedLabel: String,
    expandedLabel: String,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (expanded) expandedLabel else collapsedLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SectionList(sections: List<MetricSection>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sections.forEach { MetricSectionBlock(it) }
    }
}

@Composable
private fun WeekStrip(
    conditions: SpotConditions,
    settings: AppSettings,
    onDayClick: (DailyConditions) -> Unit
) {
    val today = LocalDate.now()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(conditions.days, key = { it.date }) { day ->
            DayCell(
                day = day,
                verdict = conditions.dailyVerdict(day, settings),
                sportType = conditions.spot.sportType,
                settings = settings,
                today = today,
                onClick = { onDayClick(day) }
            )
        }
    }
}

@Composable
private fun DayCell(
    day: DailyConditions,
    verdict: Verdict,
    sportType: SportType,
    settings: AppSettings,
    today: LocalDate,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(verdict.suitability.color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            shortDayLabel(day.date, today),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(weatherCodeEmoji(day.weatherCode), fontSize = 16.sp)
        Text(
            day.headline(sportType, settings),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        day.temperatureMaxC?.let {
            Text(
                it.formatTemp(settings.tempUnit),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            verdict.score?.toString() ?: "–",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = verdict.suitability.color,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(3.dp))
        Column(
            Modifier
                .height(5.dp)
                .width(30.dp)
                .clip(RoundedCornerShape(50))
                .background(verdict.suitability.color)
        ) {}
    }
}

/** The single figure that best characterises a day for this sport. */
private fun DailyConditions.headline(sportType: SportType, settings: AppSettings): String =
    when (sportType) {
        SportType.SURF -> waveHeightM?.formatWave(settings.waveUnit) ?: "–"
        SportType.WINDSURF -> windSpeedKmh?.formatWind(settings.windUnit) ?: "–"
        SportType.SKI -> when {
            (snowfallCm ?: 0.0) >= 0.5 -> "+${snowfallCm!!.formatSnow(settings.snowUnit)}"
            snowDepthCm != null -> snowDepthCm.formatSnowDepth(settings.snowUnit)
            else -> "–"
        }
    }
