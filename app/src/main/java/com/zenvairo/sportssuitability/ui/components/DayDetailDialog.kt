package com.zenvairo.sportssuitability.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenvairo.sportssuitability.alerts.dayOfWeekLabel
import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.prefs.AppSettings
import com.zenvairo.sportssuitability.data.prefs.formatSnowDepth
import com.zenvairo.sportssuitability.data.prefs.formatTemp
import com.zenvairo.sportssuitability.data.prefs.formatWave
import com.zenvairo.sportssuitability.data.prefs.formatWind
import com.zenvairo.sportssuitability.data.repository.DailyConditions
import com.zenvairo.sportssuitability.data.repository.HourlyConditions
import com.zenvairo.sportssuitability.data.repository.SpotConditions
import com.zenvairo.sportssuitability.data.repository.Suitability
import com.zenvairo.sportssuitability.data.repository.bestWindow
import com.zenvairo.sportssuitability.data.repository.dailyVerdict
import com.zenvairo.sportssuitability.data.repository.hourlyVerdict
import com.zenvairo.sportssuitability.data.repository.weatherCodeEmoji
import com.zenvairo.sportssuitability.ui.daySections
import com.zenvairo.sportssuitability.ui.color
import java.time.LocalDate

/** Everything known about one forecast day, including the hour-by-hour breakdown. */
@Composable
fun DayDetailDialog(
    conditions: SpotConditions,
    day: DailyConditions,
    settings: AppSettings,
    onDismiss: () -> Unit
) {
    val verdict = conditions.dailyVerdict(day, settings)
    val hours = conditions.hours.filter { it.date == day.date }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(fullDayLabel(day.date), style = MaterialTheme.typography.titleMedium)
                Text(
                    conditions.spot.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuitabilityBadge(verdict.suitability, verdict.score)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (verdict.canGo) "Puedes ir" else "Mejor no ir",
                        style = MaterialTheme.typography.bodyMedium,
                        color = verdict.suitability.color
                    )
                }
                verdict.score?.let {
                    Spacer(Modifier.height(8.dp))
                    ScoreBar(it, verdict.suitability.color)
                }

                conditions.bestWindow(day.date, settings)?.let { (from, to) ->
                    Text(
                        "Mejor franja: $from – $to",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                ReasonList("Por qué no", verdict.blockers, Suitability.NO_GO.color)
                ReasonList("A favor", verdict.highlights, Suitability.GOOD.color)
                ReasonList("Ojo", verdict.warnings, Suitability.FAIR.color)

                if (hours.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text("Hora a hora", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    HourStrip(conditions, hours, settings)
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                SectionList(conditions.daySections(day, settings))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun HourStrip(
    conditions: SpotConditions,
    hours: List<HourlyConditions>,
    settings: AppSettings
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(hours, key = { it.time }) { hour ->
            val verdict = conditions.hourlyVerdict(hour, settings)
            Column(
                modifier = Modifier
                    .width(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(verdict.suitability.color.copy(alpha = 0.14f))
                    .padding(vertical = 6.dp, horizontal = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(hour.hourLabel, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                Text(weatherCodeEmoji(hour.weatherCode), fontSize = 13.sp)
                Text(
                    hour.headline(conditions.spot.sportType, settings),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                hour.temperatureC?.let {
                    Text(
                        it.formatTemp(settings.tempUnit),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(3.dp))
                Column(
                    Modifier
                        .height(4.dp)
                        .width(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(verdict.suitability.color)
                ) {}
            }
        }
    }
}

private fun HourlyConditions.headline(sportType: SportType, settings: AppSettings): String =
    when (sportType) {
        SportType.SURF -> waveHeightM?.formatWave(settings.waveUnit) ?: "–"
        SportType.WINDSURF -> windSpeedKmh?.formatWind(settings.windUnit) ?: "–"
        SportType.SKI -> snowDepthCm?.formatSnowDepth(settings.snowUnit)
            ?: windSpeedKmh?.formatWind(settings.windUnit) ?: "–"
    }

private fun fullDayLabel(date: String): String = runCatching {
    val parsed = LocalDate.parse(date)
    "${dayOfWeekLabel(parsed.dayOfWeek.value)} ${parsed.dayOfMonth}/${parsed.monthValue}"
}.getOrElse { date }
