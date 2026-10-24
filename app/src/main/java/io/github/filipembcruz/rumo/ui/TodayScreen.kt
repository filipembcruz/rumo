package io.github.filipembcruz.rumo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.rumo.data.PeriodStats

@Composable
fun TodayScreen(today: PeriodStats, week: PeriodStats, contentPadding: PaddingValues) {
    val weekAvg = week.dailyAverage
    val compare = when {
        weekAvg <= 0 -> null
        today.total > weekAvg -> "${Fmt.duration(today.total - weekAvg)} acima da sua média de 7 dias"
        else -> "${Fmt.duration(weekAvg - today.total)} abaixo da sua média de 7 dias"
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
            SectionCard {
                BigNumber(Fmt.duration(today.total), "Tempo de tela hoje", compare)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("Desbloqueios", today.unlocks.toString(), Modifier.weight(1f))
                    MiniStat("Apps usados", today.apps.size.toString(), Modifier.weight(1f))
                    MiniStat("Média 7 dias", Fmt.duration(weekAvg), Modifier.weight(1f))
                }
            }
        }
        item {
            SectionCard(title = "Últimos 7 dias") { BarChart(week.buckets) }
        }
        item {
            SectionCard(title = "Apps de hoje") {
                if (today.apps.isEmpty()) {
                    Text(
                        "Ainda não há uso registrado hoje.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val max = today.apps.firstOrNull()?.ms ?: 1L
                today.apps.take(30).forEach { AppRow(it, max) }
            }
        }
    }
}
