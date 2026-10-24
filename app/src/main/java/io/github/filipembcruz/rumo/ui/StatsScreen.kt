package io.github.filipembcruz.rumo.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.data.Equivalences
import io.github.filipembcruz.rumo.data.PeriodStats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class Period(val label: String) {
    WEEK("7 dias"), MONTH("30 dias"), YEAR("12 meses"), CUSTOM("Personalizado")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    period: Period,
    stats: PeriodStats?,
    contentPadding: PaddingValues,
    onPeriod: (Period) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val start = state.selectedStartDateMillis!!.toLocalDate()
                        val end = (state.selectedEndDateMillis ?: state.selectedStartDateMillis!!).toLocalDate()
                        showPicker = false
                        onCustomRange(start, end)
                    },
                ) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
        ) {
            DateRangePicker(state = state, modifier = Modifier.weight(1f))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Period.entries.forEach { p ->
                    FilterChip(
                        selected = p == period,
                        onClick = { if (p == Period.CUSTOM) showPicker = true else onPeriod(p) },
                        label = { Text(p.label) },
                    )
                }
            }
        }
        if (stats == null) return@LazyColumn

        item {
            SectionCard {
                Text(
                    Fmt.range(stats.from, stats.to),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                BigNumber(Fmt.duration(stats.total), "Tempo total", Fmt.change(stats.total, stats.previousTotal))
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    MiniStat("Média por dia", Fmt.duration(stats.dailyAverage), Modifier.weight(1f))
                    MiniStat("Desbloqueios", stats.unlocks.toString(), Modifier.weight(1f))
                    MiniStat("Dias com dados", stats.trackedDays.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))
                BarChart(stats.buckets)
            }
        }

        item { EquivalenceCard(stats) }

        item {
            SectionCard(title = "Apps no período") {
                if (stats.apps.isEmpty()) {
                    Text(
                        "Sem dados neste período.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val max = stats.apps.firstOrNull()?.ms ?: 1L
                stats.apps.forEach { AppRow(it, max) }
            }
        }
    }
}

@Composable
private fun EquivalenceCard(stats: PeriodStats) {
    val periodHours = Fmt.hours(stats.total)
    val yearHours = Fmt.hours(stats.dailyAverage) * 365
    SectionCard(title = "Esse tempo poderia virar…") {
        Text(
            "No período: ${"%.0f".format(periodHours)} h  ·  No ritmo atual, em 1 ano: ${"%.0f".format(yearHours)} h",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Equivalences.all.forEachIndexed { i, e ->
            if (i > 0) HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text(e.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Column {
                    Text(
                        Equivalences.describe(e, periodHours),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "em 1 ano: ${Equivalences.describe(e, yearHours)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            "Estimativas aproximadas, só para dar perspectiva.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
