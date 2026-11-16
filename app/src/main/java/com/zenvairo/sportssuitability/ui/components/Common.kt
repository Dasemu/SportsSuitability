package com.zenvairo.sportssuitability.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenvairo.sportssuitability.data.repository.Suitability
import com.zenvairo.sportssuitability.ui.Metric
import com.zenvairo.sportssuitability.ui.MetricSection
import com.zenvairo.sportssuitability.ui.color

/** The coloured "Ideal / No ir" pill, optionally carrying the 0–100 score. */
@Composable
fun SuitabilityBadge(suitability: Suitability, score: Int? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(suitability.color.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            suitability.label,
            color = suitability.color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (score != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                "$score",
                color = suitability.color.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** A thin horizontal meter showing the score out of 100. */
@Composable
fun ScoreBar(score: Int, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
    ) {
        Spacer(
            Modifier
                .fillMaxWidth(score.coerceIn(0, 100) / 100f)
                .height(6.dp)
                .background(color)
        )
    }
}

@Composable
fun MetricChip(metric: Metric) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            metric.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            metric.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricChips(metrics: List<Metric>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        metrics.forEach { MetricChip(it) }
    }
}

@Composable
fun MetricSectionBlock(section: MetricSection) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            section.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        MetricChips(section.metrics)
    }
}

/** A bulleted reason list, tinted to match its meaning (blocker, warning, highlight). */
@Composable
fun ReasonList(title: String, reasons: List<String>, color: Color) {
    if (reasons.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = color)
        reasons.forEach { reason ->
            Text(
                "• $reason",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
